package com.modernbank.transaction_service.exception;

public class DuplicateRequestException extends BusinessException {
    public DuplicateRequestException(String message) {
        super(message);
    }
}