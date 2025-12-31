package com.modernbank.transaction_service.exception;

import lombok.Getter;

public class ProcessFailedException extends BusinessException{

    @Getter
    private String message;

    public ProcessFailedException(String message){
        super(message);
        this.message = message;
    }
}
