package com.example.supportorders.application;

public class SupportOrderNotFoundException extends RuntimeException {

    public SupportOrderNotFoundException(long id) {
        super("No existe la orden de soporte con id " + id);
    }
}
