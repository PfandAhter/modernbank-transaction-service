package com.modernbank.transaction_service.service;

import com.modernbank.transaction_service.model.TransactionListModel;
import com.modernbank.transaction_service.api.request.GetAllTransactionsRequest;

public interface TransactionService {
    TransactionListModel getAllTransactionsByAccountId(GetAllTransactionsRequest request);
}