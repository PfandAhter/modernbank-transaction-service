package com.modernbank.transaction_service.api.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter

public class BaseRequest {

    @JsonIgnore
    private LocalDateTime localDateTime;

    private String token;

    private String userId;

    @JsonIgnore
    private String userEmail;

    @JsonIgnore
    private String userRole;

    @JsonIgnore
    private String correlationId;
}