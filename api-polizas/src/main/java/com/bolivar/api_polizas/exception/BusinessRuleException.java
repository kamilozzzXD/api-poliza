package com.bolivar.api_polizas.exception;

// Excepcion para violaciones de reglas de negocio
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
