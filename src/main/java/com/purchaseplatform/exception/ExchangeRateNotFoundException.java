package com.purchaseplatform.exception;

public class ExchangeRateNotFoundException extends RuntimeException {

    public ExchangeRateNotFoundException(String message) {
        super(message);
    }
}