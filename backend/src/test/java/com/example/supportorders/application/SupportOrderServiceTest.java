package com.example.supportorders.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.supportorders.domain.Category;
import com.example.supportorders.domain.Channel;
import com.example.supportorders.domain.DefaultStatusTransitionPolicy;
import com.example.supportorders.domain.InvalidStatusTransitionException;
import com.example.supportorders.domain.OrderStatus;
import com.example.supportorders.domain.Priority;
import com.example.supportorders.domain.SupportOrder;
import com.example.supportorders.persistence.SupportOrderRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupportOrderServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Mock
    private SupportOrderRepository repository;

    private SupportOrderService service;

    @BeforeEach
    void setUp() {
        service = new SupportOrderService(repository, new DefaultStatusTransitionPolicy(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsOpenOrderWithServerTimestampAndDefaultPriority() {
        when(repository.existsByActiveReference("MOB-1001")).thenReturn(false);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SupportOrder created = service.create(newOrder("MOB-1001", null));

        assertThat(created.getStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(created.getCreatedAt()).isEqualTo(NOW);
        assertThat(created.getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    void rejectsSecondActiveOrderForSameExternalReference() {
        when(repository.existsByActiveReference("MOB-1001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(newOrder("MOB-1001", Priority.HIGH)))
                .isInstanceOf(DuplicateActiveOrderException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void changesStatusWhenTransitionIsAllowed() {
        when(repository.findById(1L)).thenReturn(Optional.of(openOrder()));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SupportOrder updated = service.changeStatus(1L, OrderStatus.IN_PROGRESS);

        assertThat(updated.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
    }

    @Test
    void rejectsTransitionNotAllowed() {
        when(repository.findById(1L)).thenReturn(Optional.of(openOrder()));

        assertThatThrownBy(() -> service.changeStatus(1L, OrderStatus.RESOLVED))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("OPEN a RESOLVED");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void returnsExistingOrder() {
        SupportOrder order = openOrder();
        when(repository.findById(1L)).thenReturn(Optional.of(order));

        assertThat(service.get(1L)).isSameAs(order);
    }

    @Test
    void failsWhenOrderDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(SupportOrderNotFoundException.class)
                .hasMessageContaining("99");
    }

    private static NewSupportOrder newOrder(String externalReference, Priority priority) {
        return new NewSupportOrder(externalReference, "CUST-001", Category.PAYMENT,
                "El pago aparece como pendiente después de varios minutos.", Channel.MOBILE_APP, priority);
    }

    private static SupportOrder openOrder() {
        return SupportOrder.open("MOB-1001", "CUST-001", Category.PAYMENT,
                "El pago aparece como pendiente.", Channel.MOBILE_APP, Priority.MEDIUM, NOW);
    }
}
