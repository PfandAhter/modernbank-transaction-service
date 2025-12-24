package com.modernbank.transaction_service.api.request;

import com.modernbank.transaction_service.model.enums.AdditionalApproveStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionAdditionalApproveRequest extends BaseRequest{
    private AdditionalApproveStatus status;
    private String transactionId;
}