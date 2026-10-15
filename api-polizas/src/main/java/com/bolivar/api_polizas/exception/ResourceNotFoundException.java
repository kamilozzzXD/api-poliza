package com.bolivar.api_polizas.exception;

// Excepcion para recursos no encontrados
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
