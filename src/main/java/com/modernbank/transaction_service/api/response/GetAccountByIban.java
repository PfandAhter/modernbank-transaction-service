package com.modernbank.transaction_service.api.response;


import com.modernbank.transaction_service.model.enums.AccountStatus;
import com.modernbank.transaction_service.model.enums.Currency;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetAccountByIban {

    private String accountId;
    private String accountName;
    private String userId;
    private String firstName;
    private String secondName;
    private String lastName;
    private String tckn;
    private String email;

    private double balance;
    private Currency currency;
    private AccountStatus status;
}