package com.modernbank.transaction_service.model.enums;

public enum TransactionStatus {
    INITIATED("INITIATED"),
    APPROVED("APPROVED"),
    FRAUD_REVIEW("FRAUD_REVIEW"),
    HOLD("HOLD"),
    BLOCKED("BLOCKED"),
    PENDING("PENDING"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED"),
    CANCELLED("CANCELLED"),
    REVERSED("REVERSED"),
    INSUFFICIENT_FUNDS("INSUFFICIENT FUNDS"),
    ACCOUNT_NOT_FOUND("ACCOUNT NOT FOUND"),
    LIMIT_EXCEEDED("LIMIT EXCEEDED"),
    FRAUD_SUSPECTED("FRAUD SUSPECTED"),
    UNKNOWN("UNKNOWN");

    private final String status;

    TransactionStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}