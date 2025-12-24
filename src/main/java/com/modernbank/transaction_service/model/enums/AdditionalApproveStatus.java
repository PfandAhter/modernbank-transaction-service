package com.modernbank.transaction_service.model.enums;

public enum AdditionalApproveStatus {
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),
    BLOCK_ACCOUNT("BLOCK_ACCOUNT");


    private final String status;

    AdditionalApproveStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}