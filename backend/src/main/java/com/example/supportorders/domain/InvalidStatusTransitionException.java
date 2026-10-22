package com.example.supportorders.domain;

public class InvalidStatusTransitionException extends RuntimeException {

    private final OrderStatus from;
    private final OrderStatus to;

    public InvalidStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("No se permite cambiar el estado de " + from + " a " + to);
        this.from = from;
        this.to = to;
    }

    public OrderStatus getFrom() {
        return from;
    }

    public OrderStatus getTo() {
        return to;
    }
}
