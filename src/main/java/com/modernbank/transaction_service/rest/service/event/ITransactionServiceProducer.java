package com.modernbank.transaction_service.rest.service.event;

import com.modernbank.transaction_service.rest.controller.request.TransferMoneyRequest;
import com.modernbank.transaction_service.rest.controller.request.WithdrawAndDepositMoneyRequest;
import com.modernbank.transaction_service.rest.controller.response.BaseResponse;

public interface ITransactionServiceProducer {
    BaseResponse withdrawMoney(WithdrawAndDepositMoneyRequest request);

    BaseResponse depositMoney(WithdrawAndDepositMoneyRequest request);

    BaseResponse transferMoney(TransferMoneyRequest request);
}
