package com.modernbank.transaction_service.api.request;

import com.modernbank.transaction_service.model.enums.InvoiceStatus;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateTransactionInvoiceStatus {
    private String transactionId;
    private String invoiceId;
    private InvoiceStatus status;
}
