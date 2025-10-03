package com.modernbank.transaction_service.model.enums;

public enum TransactionCategory {
    DEPOSIT("DEPOSIT"),
    WITHDRAWAL("WITHDRAWAL"),
    TRANSFER("TRANSFER"),
    PAYMENT("PAYMENT"),
    REFUND("REFUND"),
    FEE("FEE"),
    INTEREST("INTEREST"),
    ADJUSTMENT("ADJUSTMENT"),
    REVERSAL("REVERSAL"),
    CHARGEBACK("CHARGEBACK"),
    CASHBACK("CASHBACK"),
    TRANSFER_BY_ATM("TRANSFER BY ATM"),
    ATM_WITHDRAWAL("ATM WITHDRAWAL"),
    ATM_DEPOSIT("ATM DEPOSIT"),
    ONLINE_PURCHASE("ONLINE PURCHASE"),
    POS_PURCHASE("POS PURCHASE"),
    BILL_PAYMENT("BILL PAYMENT"),
    SALARY_CREDIT("SALARY CREDIT"),
    TAX_PAYMENT("TAX PAYMENT"),
    LOAN_PAYMENT("LOAN PAYMENT"),
    INVESTMENT("INVESTMENT"),
    DIVIDEND("DIVIDEND"),
    OTHER("OTHER");

    private final String category;

    TransactionCategory(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }
}
