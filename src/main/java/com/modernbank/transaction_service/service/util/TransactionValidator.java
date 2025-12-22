package com.modernbank.transaction_service.service.util;

public interface TransactionValidator {
    void validateSufficientFunds(String iban, double amount);

    void validateUserOwnership(String userId, String fromIBAN);
}