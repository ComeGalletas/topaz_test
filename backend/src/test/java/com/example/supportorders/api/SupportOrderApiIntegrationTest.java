package com.example.supportorders.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas HTTP de punta a punta (controller + servicio + H2 con el esquema de Flyway).
 * Cada prueba se revierte al terminar gracias a {@link Transactional}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SupportOrderApiIntegrationTest {

    private static final String BASE = "/api/support-orders";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsValidOrder() throws Exception {
        createOrder("MOB-1001", "\"priority\": \"HIGH\",")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*" + BASE + "/\\d+$")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.externalReference").value("MOB-1001"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.allowedTransitions", hasSize(2)));
    }

    @Test
    void defaultsPriorityToMediumWhenOmitted() throws Exception {
        createOrder("MOB-1002", "")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").value("MEDIUM"));
    }

    @Test
    void rejectsRequestWithMissingFieldsAndShortDescription() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalReference": "MOB-2001", "category": "PAYMENT",
                                 "description": "   corto   ", "channel": "MOBILE_APP"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.errors.customerId").value("es obligatorio"))
                .andExpect(jsonPath("$.errors.description").value("debe tener entre 10 y 500 caracteres"));
    }

    @Test
    void rejectsUnknownPriority() throws Exception {
        createOrder("MOB-2002", "\"priority\": \"URGENT\",")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.priority").value("valor inválido. Valores permitidos: LOW, MEDIUM, HIGH"));
    }

    @Test
    void rejectsSecondActiveOrderForSameExternalReference() throws Exception {
        createOrder("MOB-3001", "").andExpect(status().isCreated());

        createOrder("MOB-3001", "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Orden activa duplicada"));
    }

    @Test
    void appliesAllowedTransitions() throws Exception {
        long id = createdId("MOB-4001");

        changeStatus(id, "IN_PROGRESS")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        changeStatus(id, "RESOLVED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.allowedTransitions", hasSize(0)));
    }

    @Test
    void rejectsTransitionNotAllowed() throws Exception {
        long id = createdId("MOB-4002");

        changeStatus(id, "RESOLVED")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Transición de estado no permitida"))
                .andExpect(jsonPath("$.currentStatus").value("OPEN"))
                .andExpect(jsonPath("$.requestedStatus").value("RESOLVED"));

        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void returnsExistingOrderDetail() throws Exception {
        long id = createdId("MOB-5001");

        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.customerId").value("CUST-001"))
                .andExpect(jsonPath("$.description").isNotEmpty())
                .andExpect(jsonPath("$.channel").value("MOBILE_APP"));
    }

    @Test
    void returnsNotFoundForMissingOrder() throws Exception {
        mockMvc.perform(get(BASE + "/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Orden no encontrada"))
                .andExpect(jsonPath("$.detail").value("No existe la orden de soporte con id 999999"));
    }

    @Test
    void listsOrdersFilteredByStatus() throws Exception {
        long inProgress = createdId("MOB-6001");
        createdId("MOB-6002");
        changeStatus(inProgress, "IN_PROGRESS").andExpect(status().isOk());

        mockMvc.perform(get(BASE).param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(inProgress))
                .andExpect(jsonPath("$[0].externalReference").value("MOB-6001"))
                .andExpect(jsonPath("$[0].category").value("PAYMENT"))
                .andExpect(jsonPath("$[0].priority").isNotEmpty())
                .andExpect(jsonPath("$[0].createdAt").isNotEmpty());
    }

    @Test
    void rejectsUnknownStatusFilter() throws Exception {
        mockMvc.perform(get(BASE).param("status", "DONE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parámetro inválido"));
    }

    private ResultActions createOrder(String externalReference, String extraFields) throws Exception {
        return mockMvc.perform(post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"externalReference": "%s", "customerId": "CUST-001", "category": "PAYMENT", %s
                         "description": "El pago aparece como pendiente después de varios minutos.",
                         "channel": "MOBILE_APP"}
                        """.formatted(externalReference, extraFields)));
    }

    private long createdId(String externalReference) throws Exception {
        String body = createOrder(externalReference, "")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions changeStatus(long id, String status) throws Exception {
        return mockMvc.perform(patch(BASE + "/" + id + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"" + status + "\"}"));
    }
}
