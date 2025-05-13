package com.modernbank.transaction_service.rest.controller;

import com.modernbank.transaction_service.rest.controller.api.TransactionServiceApi;
import com.modernbank.transaction_service.rest.controller.request.TransferMoneyATMRequest;
import com.modernbank.transaction_service.rest.controller.request.TransferMoneyRequest;
import com.modernbank.transaction_service.rest.controller.request.WithdrawAndDepositMoneyRequest;
import com.modernbank.transaction_service.rest.controller.request.WithdrawFromATMRequest;
import com.modernbank.transaction_service.rest.controller.response.BaseResponse;
import com.modernbank.transaction_service.rest.service.event.ITransactionServiceProducer;
import com.modernbank.transaction_service.rest.service.event.IWithdrawFromATMServiceProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping(path = "/api/v1/transaction")
public class TransactionServiceController implements TransactionServiceApi {

    private final ITransactionServiceProducer transactionServiceProducer;

    private final IWithdrawFromATMServiceProducer withdrawFromATMServiceProducer;

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

    @Override
    public ResponseEntity<BaseResponse> withdrawMoneyFromATM(TransferMoneyATMRequest request) {
        return ResponseEntity.ok(withdrawFromATMServiceProducer.transferMoneyATM(request));
    }

    @Override
    public ResponseEntity<BaseResponse> withdrawMoneyFromATM(WithdrawFromATMRequest request) {
        return ResponseEntity.ok(withdrawFromATMServiceProducer.withdrawMoneyFromATM(request));
    }
}