package com.modernbank.transaction_service.service;

import com.modernbank.transaction_service.api.request.TransactionAdditionalApproveRequest;

public interface FraudConfirmationService {

    void confirmFraud(
            String pendingTransactionId,
            String confirmedBy,
            String reason,
            boolean blockAccount);

    void confirmLegitimate(String pendingTransactionId, String confirmedBy);

    void confirmAdditionalTransaction(TransactionAdditionalApproveRequest request);
}
