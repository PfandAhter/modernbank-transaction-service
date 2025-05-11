package com.modernbank.transaction_service.rest.controller;

import com.modernbank.transaction_service.rest.controller.api.TransactionServiceApi;
import com.modernbank.transaction_service.rest.controller.request.TransferMoneyRequest;
import com.modernbank.transaction_service.rest.controller.request.WithdrawAndDepositMoneyRequest;
import com.modernbank.transaction_service.rest.controller.response.BaseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionServiceController implements TransactionServiceApi {

    @Override
    public ResponseEntity<BaseResponse> withdrawMoney(WithdrawAndDepositMoneyRequest request) {
        return null;
    }

    @Override
    public ResponseEntity<BaseResponse> depositMoney(WithdrawAndDepositMoneyRequest request) {
        return null;
    }

    @Override
    public ResponseEntity<BaseResponse> transferMoney(TransferMoneyRequest request) {
        return null;
    }
}
