package com.example.supportorders.application;

import com.example.supportorders.domain.Category;
import com.example.supportorders.domain.Channel;
import com.example.supportorders.domain.Priority;

/**
 * Datos para crear una orden, independientes del formato HTTP.
 *
 * @param priority opcional; si es {@code null} se usa MEDIUM
 */
public record NewSupportOrder(
        String externalReference,
        String customerId,
        Category category,
        String description,
        Channel channel,
        Priority priority) {
}
