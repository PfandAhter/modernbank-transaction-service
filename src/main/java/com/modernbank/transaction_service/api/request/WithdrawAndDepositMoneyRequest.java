package com.modernbank.transaction_service.api.request;

import com.modernbank.transaction_service.model.enums.TransactionCategory;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WithdrawAndDepositMoneyRequest extends BaseRequest {
    private String accountId;

    private TransactionCategory category;

    private double amount;
}