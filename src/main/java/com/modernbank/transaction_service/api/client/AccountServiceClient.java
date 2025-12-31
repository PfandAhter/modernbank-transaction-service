package com.modernbank.transaction_service.api.client;

import com.modernbank.transaction_service.api.response.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "account-service", url = "${feign.client.account-service.url}")
public interface AccountServiceClient {

        @GetMapping(path = "${feign.client.account-service.extractFromIBAN}")
        GetAccountByIban getAccountByIban(@RequestParam(value = "iban") String iban);

        @GetMapping(path = "${feign.client.account-service.extractFromId}")
        GetAccountByIdResponse getAccountById(@RequestParam(value = "accountId") String accountId);

        @GetMapping(path = "${feign.client.account-service.getAccounts}")
        GetAccountsResponse getAccounts(@RequestParam("X-User-Id") String userId);

        @PostMapping(path = "${feign.client.account-service.updateBalance}")
        BaseResponse updateBalance(@RequestParam(value = "iban") String iban,
                        @RequestParam(value = "balance") double balance);


        @GetMapping(path = "${feign.client.account-service.getProfile}")
        AccountProfileResponse getAccountProfileByAccountId(@RequestParam(value = "accountId") String accountId);

        @PostMapping(path = "${feign.client.account-service.holdAccount}")
        BaseResponse holdAccount(@RequestParam(value = "accountId") String accountId);

        @PostMapping(path = "${feign.client.account-service.updateLimit}")
        BaseResponse updateLimit(
                @RequestParam(value = "accountId") String accountId,
                @RequestParam(value = "amount") Double amount,
                @RequestParam(value = "category") String category
        );

        @GetMapping(path = "${feign.client.account-service.isBlacklisted}")
        Boolean isReceiverBlacklisted(@RequestParam(value = "iban") String iban);

        @GetMapping(path = "${feign.client.account-service.isBlocked}")
        Boolean isAccountBlocked(@RequestParam(value = "accountId") String accountId);

        /**
         * Confirm fraud for a user - increments previousFraudCount.
         * Called when fraud is manually confirmed by analyst or user reports
         * unauthorized tx.
         */
        @PostMapping(path = "${feign.client.account-service.confirmFraud}")
        BaseResponse confirmFraud(
                        @RequestParam(value = "accountId") String accountId,
                        @RequestParam(value = "reason") String reason);

        @PostMapping(path = "${feign.client.account-service.updatePreviousFraudFlag}")
        BaseResponse updatePreviousFraudFlag(
                        @RequestParam(value = "accountId") String accountId,
                        @RequestParam(value = "isFraud") Boolean flag);

}