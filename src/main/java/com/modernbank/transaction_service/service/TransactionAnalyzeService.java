package com.modernbank.transaction_service.service;

import com.modernbank.transaction_service.api.request.AnalyzeTransactionRequest;
import com.modernbank.transaction_service.model.TransactionAnalyzeModel;

public interface TransactionAnalyzeService {

    TransactionAnalyzeModel analyzeUserTransactionHistory(AnalyzeTransactionRequest request);
}
