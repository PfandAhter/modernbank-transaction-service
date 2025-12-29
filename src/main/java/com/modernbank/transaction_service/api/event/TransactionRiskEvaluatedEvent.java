package com.modernbank.transaction_service.api.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event emitted after fraud decision is made.
 * This event feeds the credit score model and user risk profile.
 * Transaction Service does NOT calculate credit score, only emits signals.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionRiskEvaluatedEvent {

    private final String event = "TRANSACTION_RISK_EVALUATED";

    private String transactionId;
    private String pendingTransactionId;
    private String userId;
    private String accountId;

    // ML signals
    private Double riskScore;
    private String riskLevel;
    private Double amountToBalanceRatio;
    private Boolean newReceiverFlag;

    // Decision outcome
    private String finalDecision; // APPROVE, HOLD, HOLD_STRONG_AUTH, BLOCK
    private String decisionReason;

    // Confirmation tracking
    private String confirmationResult;
    private Long timeToConfirmMs;

    // Transaction context
    private Double amount;
    private String currency;
    private String channel;

    private LocalDateTime timestamp;
}
