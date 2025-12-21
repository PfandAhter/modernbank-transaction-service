package com.modernbank.transaction_service.model;

import com.modernbank.transaction_service.model.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class TransactionErrorEvent {
    private String transactionId;
    private String userId;
    private String errorType;
    private String errorCode;
    private String errorMessage;
    private TransactionType transactionType;
    private LocalDateTime timestamp;
    private Map<String, Object> context;
}