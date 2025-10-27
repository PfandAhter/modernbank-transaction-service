package com.modernbank.transaction_service.api.response;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor

public class ErrorResponse {

    private HttpStatus status;

    private String error;

    private String description;

    private LocalDateTime timestamp;
}