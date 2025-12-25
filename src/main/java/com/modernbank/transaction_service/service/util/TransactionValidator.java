package com.modernbank.transaction_service.service.util;

import com.modernbank.transaction_service.api.request.TransferMoneyRequest;

public interface TransactionValidator {
    void validateTransferMoney(TransferMoneyRequest request);

    //void validateSufficientFunds(String iban, double amount);

    //void validateUserOwnership(String userId, String fromIBAN);
}