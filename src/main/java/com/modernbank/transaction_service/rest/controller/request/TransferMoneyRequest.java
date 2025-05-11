package com.modernbank.transaction_service.rest.controller.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferMoneyRequest {
    private String fromAccountId;

    private String toAccountId;

    private double amount;

    private String description;

    private String toFirstName;

    private String toSecondName;

    private String toLastName;
}
