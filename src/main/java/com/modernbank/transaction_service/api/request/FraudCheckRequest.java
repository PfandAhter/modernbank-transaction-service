package com.modernbank.transaction_service.api.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for Fraud ML Service evaluation.
 * Contains transaction details and account profile data.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FraudCheckRequest {

    private String transactionId;
    private String userId;
    private Double transactionAmount;
    private String transactionType;
    private String merchantCategory;
    private String cardType;
    private Integer cardAgeMonths;
    private Double accountBalanceBefore;
    private Double avgTransactionAmount7d;
    private Integer transactionCount24h;
    private Integer transactionCount7d;
    private Boolean previousFraudFlag;
    private Boolean isNewReceiver;
    private Boolean isWeekend;
    private LocalDateTime timestamp;
    private String pendingTransactionId;
    private Integer previousFraudCount;

    // Behavioral signals
    private Double amountToBalanceRatio;
}