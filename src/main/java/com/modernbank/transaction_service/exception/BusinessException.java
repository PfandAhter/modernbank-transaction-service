package com.modernbank.transaction_service.exception;


import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final String message;
    private final Object[] args;

    public BusinessException(String message) {
        super(message);
        this.message = message;
        this.args = null;
    }

    public BusinessException(String message, Object... args) {
        super(message);
        this.message = message;
        this.args = args;
    }
}