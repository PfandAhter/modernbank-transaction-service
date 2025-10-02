package com.modernbank.transaction_service.model.enums;

public enum TransactionType {
    INCOME("INCOME"),
    EXPENSE("EXPENSE");

    private final String transactionType;

    TransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getTransactionType() {
        return transactionType;
    }
}