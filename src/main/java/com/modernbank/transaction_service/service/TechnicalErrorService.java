package com.modernbank.transaction_service.service;

import com.modernbank.transaction_service.api.request.TransferMoneyRequest;

public interface TechnicalErrorService {
    void handleBusinessError(
            //TransferMoneyRequest request,
            String transactionId,
            String userId,
            String errorCode,    // Örn: H-0004
            Object... args);

    void handleTechnicalError(
            //TransferMoneyRequest request,
            String errorCode, // Genelde teknik hataların kodu sabittir (örn: TECH-500)
            Exception exception);
}