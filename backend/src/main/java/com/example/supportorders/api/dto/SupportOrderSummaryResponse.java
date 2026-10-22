package com.example.supportorders.api.dto;

import com.example.supportorders.domain.Category;
import com.example.supportorders.domain.OrderStatus;
import com.example.supportorders.domain.Priority;
import com.example.supportorders.domain.SupportOrder;
import java.time.Instant;

/** Elemento del listado: solo lo necesario para la tabla del operador. */
public record SupportOrderSummaryResponse(
        Long id,
        String externalReference,
        Category category,
        Priority priority,
        OrderStatus status,
        Instant createdAt) {

    public static SupportOrderSummaryResponse from(SupportOrder order) {
        return new SupportOrderSummaryResponse(order.getId(), order.getExternalReference(), order.getCategory(),
                order.getPriority(), order.getStatus(), order.getCreatedAt());
    }
}
