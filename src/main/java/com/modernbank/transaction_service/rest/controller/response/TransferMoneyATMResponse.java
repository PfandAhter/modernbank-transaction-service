package com.modernbank.transaction_service.rest.controller.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TransferMoneyATMResponse extends BaseResponse{
    private String senderName;
    private String description;
    private Double amount;
}