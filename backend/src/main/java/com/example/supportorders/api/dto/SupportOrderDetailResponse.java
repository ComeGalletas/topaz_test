package com.example.supportorders.api.dto;

import com.example.supportorders.domain.Category;
import com.example.supportorders.domain.Channel;
import com.example.supportorders.domain.OrderStatus;
import com.example.supportorders.domain.Priority;
import com.example.supportorders.domain.SupportOrder;
import java.time.Instant;
import java.util.List;

/**
 * Detalle completo de una orden.
 *
 * @param allowedTransitions estados a los que puede pasar ahora; el cliente lo usa para mostrar solo acciones válidas
 */
public record SupportOrderDetailResponse(
        Long id,
        String externalReference,
        String customerId,
        Category category,
        String description,
        Channel channel,
        Priority priority,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<OrderStatus> allowedTransitions) {

    public static SupportOrderDetailResponse from(SupportOrder order, List<OrderStatus> allowedTransitions) {
        return new SupportOrderDetailResponse(order.getId(), order.getExternalReference(), order.getCustomerId(),
                order.getCategory(), order.getDescription(), order.getChannel(), order.getPriority(),
                order.getStatus(), order.getCreatedAt(), order.getUpdatedAt(), allowedTransitions);
    }
}
