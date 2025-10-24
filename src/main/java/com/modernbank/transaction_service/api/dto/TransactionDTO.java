package com.modernbank.transaction_service.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter

public class TransactionDTO {

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

    private String receiverFullName;
}