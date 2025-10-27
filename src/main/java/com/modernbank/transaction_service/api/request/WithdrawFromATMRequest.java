package com.modernbank.transaction_service.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WithdrawFromATMRequest {

    private String atmId;

    private String iban;

    private String tckn;
}