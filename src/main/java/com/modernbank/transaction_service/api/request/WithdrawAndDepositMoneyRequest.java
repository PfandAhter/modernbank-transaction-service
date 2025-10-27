package com.modernbank.transaction_service.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WithdrawAndDepositMoneyRequest extends BaseRequest {
    private String accountId;
    private double amount;
}