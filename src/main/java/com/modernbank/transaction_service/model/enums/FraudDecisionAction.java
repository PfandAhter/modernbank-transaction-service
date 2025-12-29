package com.modernbank.transaction_service.model.enums;

/**
 * Final decision actions from the Fraud Decision Engine.
 * APPROVE: Transaction proceeds normally
 * HOLD: Transaction paused, awaiting user confirmation (MEDIUM risk)
 * HOLD_STRONG_AUTH: Transaction paused, requires OTP/biometric (HIGH risk first
 * time)
 * BLOCK: Transaction blocked, account may be held (repeated HIGH risk or
 * confirmed fraud)
 */
public enum FraudDecisionAction {
    APPROVE("APPROVE"),
    HOLD("HOLD"),
    HOLD_STRONG_AUTH("HOLD_STRONG_AUTH"),
    BLOCK("BLOCK");

    private final String action;

    FraudDecisionAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}
