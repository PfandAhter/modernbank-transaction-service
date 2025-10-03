package com.modernbank.transaction_service.exception;

import lombok.Getter;

public class ProcessFailedException extends RuntimeException{

    @Getter
    private String message;

    public ProcessFailedException(){
        super();
        this.message = null;
    }

    public ProcessFailedException(String message){
        super();
        this.message = message;
    }
}
