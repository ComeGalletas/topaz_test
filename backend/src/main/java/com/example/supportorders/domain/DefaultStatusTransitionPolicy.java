package com.example.supportorders.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Reglas del enunciado:
 * <pre>
 * OPEN        -> IN_PROGRESS | CANCELLED
 * IN_PROGRESS -> RESOLVED    | CANCELLED
 * RESOLVED, CANCELLED: finales
 * </pre>
 */
@Component
public class DefaultStatusTransitionPolicy implements StatusTransitionPolicy {

    private final Map<OrderStatus, Set<OrderStatus>> transitions = new EnumMap<>(Map.of(
            OrderStatus.OPEN, Set.of(OrderStatus.IN_PROGRESS, OrderStatus.CANCELLED),
            OrderStatus.IN_PROGRESS, Set.of(OrderStatus.RESOLVED, OrderStatus.CANCELLED),
            OrderStatus.RESOLVED, Set.of(),
            OrderStatus.CANCELLED, Set.of()));

    @Override
    public Set<OrderStatus> allowedTransitions(OrderStatus from) {
        return transitions.getOrDefault(from, Set.of());
    }
}
