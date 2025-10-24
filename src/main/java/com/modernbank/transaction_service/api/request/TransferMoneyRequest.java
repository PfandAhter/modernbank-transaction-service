package com.modernbank.transaction_service.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferMoneyRequest {
    private String fromIBAN;

    private String toIBAN;

    private double amount;

    private String description;

    private String toFirstName;

    private String toSecondName;

    private String toLastName;

    private String byAi;
}