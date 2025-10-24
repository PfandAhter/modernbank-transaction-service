package com.modernbank.transaction_service.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.modernbank.transaction_service.model.enums.*;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "account_id")
    @Nonnull
    private String accountId;

    @Column(name = "amount")
    @Nonnull
    private Double amount;

    @Column(name = "currency")
    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column(name = "sender_first_name")
    private String senderFirstName;

    @Column(name = "sender_second_name")
    private String senderSecondName;

    @Column(name = "sender_last_name")
    private String senderLastName;

    @Column(name = "receiver_first_name")
    private String receiverFirstName;

    @Column(name = "receiver_second_name")
    private String receiverSecondName;

    @Column(name = "receiver_last_name")
    private String receiverLastName;

    @Column(name = "receiver_tckn")
    private String receiverTckn;

    @Column(name = "receiver_iban")
    private String receiverIban;

    @Column(name = "type")
    @Nonnull
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(name = "channel")
    @Nonnull
    @Enumerated(EnumType.STRING)
    private TransactionChannel channel;

    @Column(name = "category")
    @Nonnull
    @Enumerated(EnumType.STRING)
    private TransactionCategory category;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @Nonnull
    private TransactionStatus status;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "timestamp")
    @Nonnull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime date; // ISO 8601 format

    @Column(name = "updated_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime updatedDate;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "transaction_code")
    private String transactionCode;

    @Column(name = "is_recurring")
    private Boolean isRecurring;

    @Column(name = "ai_final_category")
    private String aiFinalCategory;
}