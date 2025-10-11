package com.modernbank.transaction_service.rest.service;

import com.modernbank.transaction_service.model.TransactionListModel;
import com.modernbank.transaction_service.rest.controller.request.GetAllTransactionsRequest;

import java.time.LocalDateTime;

public interface TransactionService {
    TransactionListModel getAllTransactionsByAccountId(GetAllTransactionsRequest request);
}