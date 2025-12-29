package com.modernbank.transaction_service.model.enums;

/**
 * Fraud decision action taken on a transaction.
 * 
 * Decision flow:
 * - APPROVE: Proceed with Kafka flow (start-transfer-money)
 * - HOLD: Save transaction, do NOT start Kafka flow, trigger user confirmation
 * - BLOCK: Reject immediately, do NOT start Kafka flow, increment fraud counter
 */
public enum FraudDecision {

    APPROVE("APPROVE"),
    HOLD("HOLD"),
    BLOCK("BLOCK");

    private final String value;

    FraudDecision(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static FraudDecision fromRiskLevel(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case LOW -> APPROVE;
            case MEDIUM -> HOLD;
            case HIGH -> BLOCK;
        };
    }
}
