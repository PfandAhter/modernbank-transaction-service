package com.modernbank.transaction_service.service.util;

import com.modernbank.transaction_service.api.request.TransferMoneyATMRequest;
import com.modernbank.transaction_service.api.request.TransferMoneyRequest;
import com.modernbank.transaction_service.api.request.WithdrawAndDepositMoneyRequest;

public interface TransactionValidator {
    void validateTransferMoney(TransferMoneyRequest request);

    void validateDepositMoneyDailyLimit(WithdrawAndDepositMoneyRequest request);

    void validateWithdrawMoneyDailyLimit(WithdrawAndDepositMoneyRequest request);

    void validateDepositMoneyATMLimit(TransferMoneyATMRequest request);

    //void validateSufficientFunds(String iban, double amount);

    //void validateUserOwnership(String userId, String fromIBAN);
}