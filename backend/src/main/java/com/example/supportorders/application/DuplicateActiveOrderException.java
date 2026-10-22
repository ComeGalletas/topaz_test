package com.example.supportorders.application;

public class DuplicateActiveOrderException extends RuntimeException {

    public DuplicateActiveOrderException(String externalReference) {
        super("Ya existe una orden activa (OPEN o IN_PROGRESS) para la referencia externa " + externalReference);
    }
}
