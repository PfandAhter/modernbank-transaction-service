package com.modernbank.transaction_service.rest.controller.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferMoneyATMRequest {

    private String atmId;
    private String senderIban;

    private String receiverIban;
    private String receiverTckn;

    private String receiverFirstName;
    private String receiverSecondName;
    private String receiverLastName;
    private Double amount;
    private String description;
}