package com.modernbank.transaction_service.model;

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

    private String receiverTCKN;

    private String receiverFirstName;

    private String receiverSecondName;

    private String receiverLastName;

    private String receiverFullName;
}