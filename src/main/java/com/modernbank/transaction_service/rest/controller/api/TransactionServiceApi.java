package com.modernbank.transaction_service.rest.controller.api;

import com.modernbank.transaction_service.rest.controller.request.TransferMoneyATMRequest;
import com.modernbank.transaction_service.rest.controller.request.TransferMoneyRequest;
import com.modernbank.transaction_service.rest.controller.request.WithdrawAndDepositMoneyRequest;
import com.modernbank.transaction_service.rest.controller.response.BaseResponse;
import com.modernbank.transaction_service.rest.controller.response.TransferMoneyATMResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

public interface TransactionServiceApi {

    @PostMapping(path = "/withdraw", produces = "application/json", consumes = "application/json")
    ResponseEntity<BaseResponse> withdrawMoney(WithdrawAndDepositMoneyRequest request);

    @PostMapping(path = "/deposit", produces = "application/json", consumes = "application/json")
    ResponseEntity<BaseResponse> depositMoney(WithdrawAndDepositMoneyRequest request);

    @PostMapping(path = "/transfer", produces = "application/json", consumes = "application/json")
    ResponseEntity<BaseResponse> transferMoney(TransferMoneyRequest request);

    @PostMapping(path = "/transfer/atm", produces = "application/json", consumes = "application/json")
    ResponseEntity<BaseResponse> transferMoneyATM(TransferMoneyATMRequest request);

    @PostMapping(path = "/withdraw/atm", produces = "application/json", consumes = "application/json")
    ResponseEntity<TransferMoneyATMResponse> transferMoneyATM(TransferMoneyRequest request);
}