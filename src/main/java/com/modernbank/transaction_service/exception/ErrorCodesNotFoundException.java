package com.modernbank.transaction_service.exception;


import lombok.Getter;

import java.time.LocalDateTime;

public class ErrorCodesNotFoundException extends RuntimeException{

    @Getter
    private String message;

    @Getter
    private LocalDateTime timestamp;

    public ErrorCodesNotFoundException(String message, LocalDateTime timestamp){
        super(message);
        this.message = message;
        this.timestamp = timestamp;
    }
}
