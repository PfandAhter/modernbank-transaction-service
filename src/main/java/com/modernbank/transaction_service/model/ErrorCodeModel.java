package com.modernbank.transaction_service.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorCodeModel {
    private String id;

    private String error;

    private String description;
}