package com.example.supportorders.domain;

import java.util.Set;

/**
 * Estrategia que decide qué cambios de estado están permitidos.
 * <p>
 * La entidad y el servicio dependen de esta abstracción, no de una tabla de reglas concreta: cambiar
 * o ampliar las reglas (p. ej. reabrir órdenes, reglas por rol) es añadir otra implementación sin
 * tocar el caso de uso.
 */
public interface StatusTransitionPolicy {

    /** Estados a los que se puede pasar desde {@code from}. Vacío si {@code from} es final. */
    Set<OrderStatus> allowedTransitions(OrderStatus from);

    default boolean canTransition(OrderStatus from, OrderStatus to) {
        return allowedTransitions(from).contains(to);
    }
}
