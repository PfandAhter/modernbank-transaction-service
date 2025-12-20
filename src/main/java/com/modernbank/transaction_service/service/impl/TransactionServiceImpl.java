package com.modernbank.transaction_service.service.impl;

import com.modernbank.transaction_service.api.request.UpdateTransactionInvoiceStatus;
import com.modernbank.transaction_service.entity.Transaction;
import com.modernbank.transaction_service.model.TransactionListModel;
import com.modernbank.transaction_service.model.TransactionModel;
import com.modernbank.transaction_service.model.enums.InvoiceStatus;
import com.modernbank.transaction_service.model.enums.TransactionType;
import com.modernbank.transaction_service.repository.TransactionRepository;
import com.modernbank.transaction_service.api.request.GetAllTransactionsRequest;
import com.modernbank.transaction_service.service.TransactionService;
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
                .map(transaction -> {
                    String receiverInfo;
                    String receiverTckn = transaction.getReceiverTckn();
                    TransactionModel model = mapToModel(transaction);

                    if (receiverTckn != null && !receiverTckn.isEmpty()) {
                        receiverInfo = receiverTckn.substring(0, Math.min(3, receiverTckn.length())) + "********";
                        model.setReceiverTCKN(receiverInfo);
                    } else {
                        String firstName = transaction.getReceiverFirstName();
                        String secondName = transaction.getReceiverSecondName();
                        String lastName = transaction.getReceiverLastName();

                        StringBuilder sb = new StringBuilder();
                        if (firstName != null && !firstName.isEmpty()) {
                            sb.append(firstName.substring(0, Math.min(3, firstName.length())) + "**** ");
                        }
                        if (secondName != null && !secondName.isEmpty()) {
                            sb.append(" ").append(secondName.substring(0, Math.min(3, secondName.length()))+ "**** ");
                        }
                        if (lastName != null && !lastName.isEmpty()) {
                            sb.append(" ").append(lastName.substring(0, Math.min(3, lastName.length()))+ "**** ");
                        }
                        receiverInfo = sb.toString().trim();

                        model.setReceiverFullName(receiverInfo);
                    }

                    Object invStatusObj = transaction.getInvoiceStatus();
                    if (invStatusObj != null) {
                        if (invStatusObj instanceof InvoiceStatus) {
                            model.setInvoiceStatus((InvoiceStatus) invStatusObj);
                        } else if (invStatusObj instanceof Enum) {
                            try {
                                model.setInvoiceStatus(InvoiceStatus.valueOf(((Enum<?>) invStatusObj).name()));
                            } catch (IllegalArgumentException ignored) {

                            }
                        } else if (invStatusObj instanceof Number) {
                            int idx = ((Number) invStatusObj).intValue();
                            InvoiceStatus[] values = InvoiceStatus.values();
                            if (idx >= 0 && idx < values.length) {
                                model.setInvoiceStatus(values[idx]);
                            }
                        } else {
                            try {
                                model.setInvoiceStatus(InvoiceStatus.valueOf(invStatusObj.toString()));
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }

                    return model;
                })
                .toList();

        return new TransactionListModel(
                transactionModels,
                transactionPage.getTotalPages(),
                transactionPage.getTotalElements()
        );
    }

    @Override
    public void updateTransactionInvoiceStatus(UpdateTransactionInvoiceStatus request) {
        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with id: " + request.getTransactionId()));

        transaction.setInvoiceStatus(request.getStatus());
        transaction.setInvoiceId(request.getInvoiceId());
        transactionRepository.save(transaction);
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
        model.setReceiverFirstName(transaction.getReceiverFirstName());
        model.setReceiverSecondName(transaction.getReceiverSecondName());
        model.setReceiverLastName(transaction.getReceiverLastName());
        model.setReceiverTCKN(transaction.getReceiverTckn());

        return model;
    }
}