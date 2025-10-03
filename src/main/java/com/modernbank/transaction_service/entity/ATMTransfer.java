package com.modernbank.transaction_service.entity;

import com.modernbank.transaction_service.model.enums.ATMTransferStatus;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "atm_transfer")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ATMTransfer implements Serializable {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(name = "atm_id")
    private String atmId;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "sender_iban")
    private String senderIban;

    @Column(name = "receiver_iban")
    private String receiverIban;

    @Column(name = "receiver_tckn")
    private String receiverTckn;

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

    @Column(name = "amount")
    private Double amount;

    @Column(name = "description")
    private String description;

    @Column(name = "active")
    private int active;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    ATMTransferStatus status;

    @Column(name = "transfer_date")
    private LocalDateTime transferDate;

    @Column(name = "update_date")
    private LocalDateTime updateDate;
}