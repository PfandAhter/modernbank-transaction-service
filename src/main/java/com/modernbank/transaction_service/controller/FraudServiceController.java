package com.modernbank.transaction_service.controller;

import com.modernbank.transaction_service.api.FraudServiceApi;
import com.modernbank.transaction_service.api.request.ConfirmFraudRequest;
import com.modernbank.transaction_service.api.request.TransactionAdditionalApproveRequest;
import com.modernbank.transaction_service.api.response.BaseResponse;
import com.modernbank.transaction_service.service.FraudConfirmationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for fraud management operations.
 * 
 * Endpoints for:
 * - Confirming fraud (analyst/user report)
 * - Confirming legitimate transactions (false positives)
 */
@RestController
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
@Slf4j
public class FraudServiceController implements FraudServiceApi {

        private final FraudConfirmationService fraudConfirmationService;

        /**
         * Confirm a transaction as fraudulent.
         * 
         * Called by:
         * - Fraud analyst after review
         * - Customer support when user reports unauthorized tx
         * - Automated chargeback processing
         * 
         * This will:
         * - Update the fraud decision record
         * - Increment user's previousFraudCount
         * - Optionally block the account
         */
        @PostMapping("/confirm")
        public ResponseEntity<BaseResponse> confirmFraud(@RequestBody ConfirmFraudRequest request) {
                log.warn("Fraud confirmation request: pendingTransactionId={}, confirmedBy={}",
                                request.getPendingTransactionId(), request.getConfirmedBy());

                fraudConfirmationService.confirmFraud(
                                request.getPendingTransactionId(),
                                request.getConfirmedBy(),
                                request.getReason(),
                                request.isBlockAccount());

                return ResponseEntity.ok(new BaseResponse("Fraud confirmed successfully"));
        }

        /**
         * Confirm a transaction as legitimate (false positive).
         * 
         * Called when user verifies they made the transaction.
         * This data is valuable for improving ML model.
         */
        @PostMapping("/confirm-legitimate/{pendingTransactionId}")
        public ResponseEntity<BaseResponse> confirmLegitimate(
                        @PathVariable String pendingTransactionId,
                        @RequestParam(defaultValue = "USER") String confirmedBy) {
                log.info("Legitimate confirmation: pendingTransactionId={}", pendingTransactionId);

                fraudConfirmationService.confirmLegitimate(pendingTransactionId, confirmedBy);

                return ResponseEntity.ok(new BaseResponse("Transaction confirmed as legitimate"));
        }

        @Override
        public BaseResponse requestAdditionalApproval(TransactionAdditionalApproveRequest request) {
                fraudConfirmationService.confirmAdditionalTransaction(request);
                return new BaseResponse("Ek onay talebi alındı");
        }
}
