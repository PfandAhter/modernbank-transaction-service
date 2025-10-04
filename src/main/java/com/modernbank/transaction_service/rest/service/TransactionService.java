package com.modernbank.transaction_service.rest.service;

import com.modernbank.transaction_service.model.TransactionListModel;

public interface TransactionService {
    TransactionListModel getAllTransactionsByAccountId(String accountId, int page, int size);
}