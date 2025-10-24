package com.modernbank.transaction_service.service.event;

import com.modernbank.transaction_service.api.request.TransferMoneyRequest;
import com.modernbank.transaction_service.api.request.WithdrawAndDepositMoneyRequest;
import com.modernbank.transaction_service.api.response.BaseResponse;

public interface ITransactionServiceProducer {
    BaseResponse withdrawMoney(WithdrawAndDepositMoneyRequest request);

    BaseResponse depositMoney(WithdrawAndDepositMoneyRequest request);

    BaseResponse transferMoney(TransferMoneyRequest request);
}
