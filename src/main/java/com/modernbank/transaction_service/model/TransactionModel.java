package com.modernbank.transaction_service.model;

import com.modernbank.transaction_service.model.enums.TransactionCategory;
import com.modernbank.transaction_service.model.enums.TransactionChannel;
import com.modernbank.transaction_service.model.enums.TransactionStatus;
import com.modernbank.transaction_service.model.enums.TransactionType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter

public class TransactionModel {

    private String id;

    private String accountId;

    private double amount;

    private String type;

    private String channel;

    private String category;

    private String status;

    private String description;

    private LocalDateTime date;
}