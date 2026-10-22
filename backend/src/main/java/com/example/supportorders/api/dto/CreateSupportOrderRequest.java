package com.example.supportorders.api.dto;

import com.example.supportorders.application.NewSupportOrder;
import com.example.supportorders.domain.Category;
import com.example.supportorders.domain.Channel;
import com.example.supportorders.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cuerpo de POST /api/support-orders. Los textos se recortan antes de validarse. */
public record CreateSupportOrderRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 50, message = "debe tener como máximo {max} caracteres")
        String externalReference,

        @NotBlank(message = "es obligatorio")
        @Size(max = 50, message = "debe tener como máximo {max} caracteres")
        String customerId,

        @NotNull(message = "es obligatorio")
        Category category,

        @NotBlank(message = "es obligatorio")
        @Size(min = 10, max = 500, message = "debe tener entre {min} y {max} caracteres")
        String description,

        @NotNull(message = "es obligatorio")
        Channel channel,

        /* Opcional: si no se envía se asume MEDIUM. */
        Priority priority) {

    public CreateSupportOrderRequest {
        externalReference = trim(externalReference);
        customerId = trim(customerId);
        description = trim(description);
    }

    public NewSupportOrder toNewSupportOrder() {
        return new NewSupportOrder(externalReference, customerId, category, description, channel, priority);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
