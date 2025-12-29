package com.modernbank.transaction_service.api.event;

import com.modernbank.transaction_service.model.enums.FraudDecisionAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal message for fraud decision result Kafka topic.
 * Contains the decision outcome from the Decision Engine.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FraudDecisionMessage {

    private String pendingTransactionId;
    private String fraudDecisionId;

    // Decision outcome
    private FraudDecisionAction action;
    private String decisionReason;

    // ML signals for logging
    private Double riskScore;
    private String riskLevel;

    // For HOLD cases
    private Long holdTimeoutMinutes;
    private Boolean requiresStrongAuth;
    private String authType; // OTP, BIOMETRIC, IN_APP

    // For BLOCK cases
    private Boolean shouldBlockAccount;
    private String blockReason;

    // Original transfer data (for routing to next stage)
    private String fromIBAN;
    private String toIBAN;
    private Double amount;
    private String description;
    private String toFirstName;
    private String toSecondName;
    private String toLastName;
    private String byAi;
}
