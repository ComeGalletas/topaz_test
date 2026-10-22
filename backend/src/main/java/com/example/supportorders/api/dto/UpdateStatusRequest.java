package com.example.supportorders.api.dto;

import com.example.supportorders.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

/** Cuerpo de PATCH /api/support-orders/{id}/status. */
public record UpdateStatusRequest(
        @NotNull(message = "es obligatorio")
        OrderStatus status) {
}
