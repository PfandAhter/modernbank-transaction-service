package com.modernbank.transaction_service.rest.service.impl;

import com.modernbank.transaction_service.api.dto.TransactionDTO;
import com.modernbank.transaction_service.entity.Transaction;
import com.modernbank.transaction_service.model.TransactionListModel;
import com.modernbank.transaction_service.model.TransactionModel;
import com.modernbank.transaction_service.model.enums.TransactionType;
import com.modernbank.transaction_service.repository.TransactionRepository;
import com.modernbank.transaction_service.rest.controller.request.GetAllTransactionsRequest;
import com.modernbank.transaction_service.rest.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor

public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    @Override
    public TransactionListModel getAllTransactionsByAccountId(GetAllTransactionsRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        TransactionType type = null;
        if (request.getType() != null && !request.getType().equalsIgnoreCase("ALL")) {
            type = TransactionType.valueOf(request.getType().toUpperCase());
        }

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = null;

        if ("WEEK".equalsIgnoreCase(request.getDateRange())) {
            startDate = endDate.minusWeeks(1);
        } else if ("MONTH".equalsIgnoreCase(request.getDateRange())) {
            startDate = endDate.minusMonths(1);
        }

        Page<Transaction> transactionPage = transactionRepository.findAllByAccountIdAndTypeAndDateBetween(
                request.getAccountId(),
                type,
                startDate,
                endDate,
                pageable
        );

        List<TransactionModel> transactionModels = transactionPage.getContent().stream()
                .map(this::mapToModel)
                .toList();

        return new TransactionListModel(
                transactionModels,
                transactionPage.getTotalPages(),
                transactionPage.getTotalElements()
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