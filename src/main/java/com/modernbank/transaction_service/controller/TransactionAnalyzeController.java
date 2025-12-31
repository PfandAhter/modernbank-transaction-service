package com.modernbank.transaction_service.controller;

import com.modernbank.transaction_service.api.request.AnalyzeTransactionRequest;
import com.modernbank.transaction_service.model.TransactionAnalyzeModel;
import com.modernbank.transaction_service.model.enums.AnalyzeRange;
import com.modernbank.transaction_service.service.TransactionAnalyzeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for transaction analysis operations.
 * Provides endpoints for fraud detection and analysis services.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionAnalyzeController {

    private final TransactionAnalyzeService transactionAnalyzeService;

    /**
     * Analyze user transactions for fraud detection.
     * Returns current and previous period transactions with historical statistics.
     *
     * @param userId        User ID to analyze transactions for
     * @param analyzeRange  Time range for analysis (LAST_7_DAYS or LAST_30_DAYS)
     * @param token         Authorization token
     * @param correlationId Correlation ID for request tracing
     * @return TransactionAnalyzeModel containing current/previous period
     *         transactions and statistics
     */
    @GetMapping("/analyze")
    public TransactionAnalyzeModel getTransactionsForAnalysis(
            @RequestParam("userId") String userId,
            @RequestParam("analyzeRange") AnalyzeRange analyzeRange,
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-Correlation-ID") String correlationId) {

        log.info("Transaction analysis requested for userId: {}, analyzeRange: {}, correlationId: {}",
                userId, analyzeRange, correlationId);

        AnalyzeTransactionRequest request = new AnalyzeTransactionRequest();
        request.setUserId(userId);
        request.setAnalyzeRange(analyzeRange);

        return transactionAnalyzeService.analyzeUserTransactionHistory(request);
    }
}
