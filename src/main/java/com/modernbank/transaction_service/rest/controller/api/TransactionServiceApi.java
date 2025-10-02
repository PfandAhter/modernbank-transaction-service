package com.modernbank.transaction_service.rest.controller.api;

import com.modernbank.transaction_service.rest.controller.request.TransferMoneyATMRequest;
import com.modernbank.transaction_service.rest.controller.request.TransferMoneyRequest;
import com.modernbank.transaction_service.rest.controller.request.WithdrawAndDepositMoneyRequest;
import com.modernbank.transaction_service.rest.controller.request.WithdrawFromATMRequest;
import com.modernbank.transaction_service.rest.controller.response.BaseResponse;
import com.modernbank.transaction_service.rest.controller.response.GetTransactionsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface TransactionServiceApi {

    @PostMapping(path = "/withdraw", produces = "application/json", consumes = "application/json")
    ResponseEntity<BaseResponse> withdrawMoney(@RequestBody WithdrawAndDepositMoneyRequest request);

    @PostMapping(path = "/deposit", produces = "application/json", consumes = "application/json")
    ResponseEntity<BaseResponse> depositMoney(@RequestBody WithdrawAndDepositMoneyRequest request);

    @PostMapping(path = "/transfer", produces = "application/json", consumes = "application/json")
    ResponseEntity<BaseResponse> transferMoney(@RequestBody TransferMoneyRequest request);

    @PostMapping(path = "/transfer/atm", produces = "application/json", consumes = "application/json")
    ResponseEntity<BaseResponse> withdrawMoneyFromATM(@RequestBody TransferMoneyATMRequest request);

    @PostMapping(path = "/withdraw/atm", produces = "application/json", consumes = "application/json")
    ResponseEntity<BaseResponse> withdrawMoneyFromATM(@RequestBody WithdrawFromATMRequest request);

    @GetMapping(path = "/transactions")
    GetTransactionsResponse getAllTransactions(@RequestParam("accountId") String accountId, @RequestParam("page") int page, @RequestParam("size") int size);
}