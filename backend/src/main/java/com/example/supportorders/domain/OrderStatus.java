package com.example.supportorders.domain;

/**
 * Ciclo de vida de una orden de soporte.
 * <p>
 * Una orden es <b>activa</b> mientras está en OPEN o IN_PROGRESS: todavía requiere trabajo del
 * equipo de operaciones. RESOLVED y CANCELLED son estados finales.
 */
public enum OrderStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CANCELLED;

    public boolean isActive() {
        return this == OPEN || this == IN_PROGRESS;
    }
}
