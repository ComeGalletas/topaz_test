package com.example.supportorders.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;

/**
 * Orden de soporte. Entidad de persistencia: nunca se expone directamente en la API (ver DTOs).
 * <p>
 * Invariante: {@code activeReference} es igual a {@code externalReference} mientras la orden está
 * activa y {@code null} cuando llega a un estado final. Una restricción UNIQUE sobre esa columna
 * impide en la base de datos que una referencia externa tenga dos órdenes activas.
 */
@Entity
@Table(name = "support_orders")
public class SupportOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_reference", nullable = false, length = 50, updatable = false)
    private String externalReference;

    @Column(name = "customer_id", nullable = false, length = 50, updatable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "active_reference", length = 50)
    private String activeReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected SupportOrder() {
        // requerido por JPA
    }

    private SupportOrder(String externalReference, String customerId, Category category,
                         String description, Channel channel, Priority priority, Instant now) {
        this.externalReference = Objects.requireNonNull(externalReference);
        this.customerId = Objects.requireNonNull(customerId);
        this.category = Objects.requireNonNull(category);
        this.description = Objects.requireNonNull(description);
        this.channel = Objects.requireNonNull(channel);
        this.priority = priority != null ? priority : Priority.MEDIUM;
        this.createdAt = Objects.requireNonNull(now);
        applyStatus(OrderStatus.OPEN, now);
    }

    /**
     * Crea una orden nueva: siempre nace en OPEN y con la fecha que provee el servidor.
     * Si no se indica prioridad se asume MEDIUM.
     */
    public static SupportOrder open(String externalReference, String customerId, Category category,
                                    String description, Channel channel, Priority priority, Instant now) {
        return new SupportOrder(externalReference, customerId, category, description, channel, priority, now);
    }

    /**
     * Cambia el estado si la política lo permite.
     *
     * @throws InvalidStatusTransitionException si la transición no está permitida
     */
    public void transitionTo(OrderStatus target, StatusTransitionPolicy policy, Instant now) {
        Objects.requireNonNull(target);
        if (!policy.canTransition(status, target)) {
            throw new InvalidStatusTransitionException(status, target);
        }
        applyStatus(target, now);
    }

    private void applyStatus(OrderStatus newStatus, Instant now) {
        this.status = newStatus;
        this.activeReference = newStatus.isActive() ? externalReference : null;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Category getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public Channel getChannel() {
        return channel;
    }

    public Priority getPriority() {
        return priority;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
