package com.modernbank.transaction_service.model.enums;

public enum ATMTransferStatus {
    PENDING("PENDING"),
    COMPLETED("COMPLETED"),
    CANCELED("CANCELED"),
    REFUNDED("REFUNDED");

    private final String transferStatus;

    ATMTransferStatus(String transferStatus) {
        this.transferStatus = transferStatus;
    }

    public String getATMTransferStatus() {
        return transferStatus;
    }
}