package com.example.supportorders.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.supportorders.domain.Category;
import com.example.supportorders.domain.Channel;
import com.example.supportorders.domain.OrderStatus;
import com.example.supportorders.domain.Priority;
import com.example.supportorders.domain.SupportOrder;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

/** Usa la base en memoria del perfil "test" con el esquema real de Flyway (no la autogenerada). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class SupportOrderRepositoryTest {

    @Autowired
    private SupportOrderRepository repository;

    @Test
    void persistsOpenOrderAndFindsItAsActive() {
        SupportOrder saved = repository.saveAndFlush(order("MOB-1"));

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.existsByActiveReference("MOB-1")).isTrue();
        assertThat(repository.findByStatus(OrderStatus.OPEN, Sort.by("createdAt"))).hasSize(1);
    }

    @Test
    void databaseRejectsSecondActiveOrderWithSameExternalReference() {
        repository.saveAndFlush(order("MOB-2"));

        assertThatThrownBy(() -> repository.saveAndFlush(order("MOB-2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static SupportOrder order(String externalReference) {
        return SupportOrder.open(externalReference, "CUST-001", Category.PAYMENT,
                "El pago aparece como pendiente.", Channel.MOBILE_APP, Priority.MEDIUM, Instant.now());
    }
}
