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

    @PostMapping("/analyze")
    public TransactionAnalyzeModel getTransactionsForAnalysis(@RequestBody AnalyzeTransactionRequest request) {

        return transactionAnalyzeService.analyzeUserTransactionHistory(request);
    }
}
