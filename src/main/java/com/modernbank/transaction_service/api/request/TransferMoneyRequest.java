package com.modernbank.transaction_service.api.request;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferMoneyRequest extends BaseRequest{

    private String senderTransactionId;

    private String receiverTransactionId;

    private String fromIBAN;

    private String toIBAN;

    private double amount;

    private String description;

    private String toFirstName;

    private String toSecondName;

    private String toLastName;

    private String byAi;

    private Boolean isConfirmed;
}