package com.example.supportorders.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.supportorders.domain.Category;
import com.example.supportorders.domain.Channel;
import com.example.supportorders.domain.OrderStatus;
import com.example.supportorders.domain.Priority;
import com.example.supportorders.domain.SupportOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Servicio contra la base real (H2 + Flyway): regla de orden activa de punta a punta. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SupportOrderServiceIntegrationTest {

    @Autowired
    private SupportOrderService service;

    @Test
    void externalReferenceCanBeReusedOnlyAfterPreviousOrderIsFinal() {
        SupportOrder first = service.create(newOrder("MOB-INT-1"));

        assertThatThrownBy(() -> service.create(newOrder("MOB-INT-1")))
                .isInstanceOf(DuplicateActiveOrderException.class);

        service.changeStatus(first.getId(), OrderStatus.IN_PROGRESS);
        service.changeStatus(first.getId(), OrderStatus.RESOLVED);

        SupportOrder second = service.create(newOrder("MOB-INT-1"));
        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(service.list(OrderStatus.OPEN)).extracting(SupportOrder::getId).contains(second.getId());
    }

    private static NewSupportOrder newOrder(String externalReference) {
        return new NewSupportOrder(externalReference, "CUST-001", Category.TECHNICAL,
                "La aplicación se cierra al abrir el historial.", Channel.MOBILE_APP, Priority.HIGH);
    }
}
