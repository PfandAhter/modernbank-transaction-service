package com.modernbank.transaction_service.rest.service.impl;

import com.modernbank.transaction_service.entity.Transaction;
import com.modernbank.transaction_service.model.TransactionListModel;
import com.modernbank.transaction_service.model.TransactionModel;
import com.modernbank.transaction_service.repository.TransactionRepository;
import com.modernbank.transaction_service.rest.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    @Override
    public TransactionListModel getAllTransactionsByAccountId(String accountId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
        Page<Transaction> transactions = transactionRepository.findByAccountId(accountId, pageable);

        return new TransactionListModel(
                transactions.getContent().stream()
                        .map(this::mapToModel)
                        .toList(),
                transactions.getTotalPages(),
                transactions.getTotalElements()
        );
    }

    private TransactionModel mapToModel(Transaction transaction) {
        TransactionModel model = new TransactionModel();
        model.setId(transaction.getId());
        model.setAccountId(transaction.getAccountId());
        model.setAmount(transaction.getAmount());
        model.setDate(transaction.getDate());
        model.setDescription(transaction.getDescription());
        model.setType(transaction.getType().getTransactionType());
        model.setChannel(transaction.getChannel().getChannel());
        model.setCategory(transaction.getCategory().getCategory());
        model.setStatus(transaction.getStatus().getStatus());

        return model;
    }
}