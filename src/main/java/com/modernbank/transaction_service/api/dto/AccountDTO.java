package com.modernbank.transaction_service.api.dto;

import com.modernbank.transaction_service.model.enums.Currency;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class AccountDTO {

    private String id;

    private String iban;

    private String userId;

    private String name;

    private double balance;

    private String firstName;

    private String secondName;

    private Double dailyTransferLimit;

    private Double dailyWithdrawLimit;

    private Double dailyDepositLimit;

    private String lastName;

    private String description;

    private Currency currency;
}