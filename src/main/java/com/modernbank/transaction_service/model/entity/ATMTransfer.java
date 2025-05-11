package com.modernbank.transaction_service.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "atm_transfer")
@Getter
@Setter

public class ATMTransfer {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receiver_iban")
    private String receiverIban;

    @Column(name = "receiver_tckn")
    private String receiverTckn;

    @Column(name = "sender_full_name")
    private String senderFullName;

    @Column(name = "transfer_amount")
    private Double transferAmount;

    @Column(name = "transfer_date")
    private LocalDateTime transferDate;
}