package com.modernbank.transaction_service.api.client;

import com.modernbank.transaction_service.api.response.AccountProfileResponse;
import com.modernbank.transaction_service.api.response.GetAccountByIban;
import com.modernbank.transaction_service.api.response.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "account-service", url = "${feign.client.account-service.url}")
public interface AccountServiceClient {

        @GetMapping(path = "${feign.client.account-service.extractFromIBAN}")
        GetAccountByIban getAccountByIban(@RequestParam(value = "iban") String iban);

        @PostMapping(path = "${feign.client.account-service.updateBalance}")
        BaseResponse updateBalance(@RequestParam(value = "iban") String iban,
                        @RequestParam(value = "balance") double balance);

        /**
         * Get account profile for fraud detection (READ-ONLY).
         * Returns account age, credit score, avg balance, previous fraud count, etc.
         */
        @GetMapping(path = "${feign.client.account-service.getProfile}")
        AccountProfileResponse getAccountProfileByAccountId(@RequestParam(value = "accountId") String accountId);

        /**
         * Hold an account due to suspicious activity.
         * Used when BLOCK decision is made.
         */
        @PostMapping(path = "${feign.client.account-service.holdAccount}")
        BaseResponse holdAccount(@RequestParam(value = "accountId") String accountId);

        /**
         * Check if a receiver IBAN is blacklisted.
         */
        @GetMapping(path = "${feign.client.account-service.isBlacklisted}")
        Boolean isReceiverBlacklisted(@RequestParam(value = "iban") String iban);

        /**
         * Confirm fraud for a user - increments previousFraudCount.
         * Called when fraud is manually confirmed by analyst or user reports
         * unauthorized tx.
         */
        @PostMapping(path = "${feign.client.account-service.confirmFraud}")
        BaseResponse confirmFraud(
                        @RequestParam(value = "accountId") String accountId,
                        @RequestParam(value = "reason") String reason);

        /**
         * Check if account is temporarily blocked.
         * Must be called BEFORE starting any transfer.
         */
        @GetMapping(path = "${feign.client.account-service.isBlocked}")
        Boolean isAccountBlocked(@RequestParam(value = "accountId") String accountId);
}