package com.modernbank.transaction_service.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferMoneyATMRequest {

    private String atmId;
    private String senderIban;

    private String senderFirstName;
    private String senderSecondName;
    private String senderLastName;

    private String receiverIban;
    private String receiverTckn;

    private String receiverFirstName;
    private String receiverSecondName;
    private String receiverLastName;
    private double amount;
    private String description;
}