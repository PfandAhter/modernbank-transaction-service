package com.modernbank.transaction_service.api.response;

import com.modernbank.transaction_service.model.enums.InvoiceStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateInvoicePDFResponse extends BaseResponse{

    private String requestId;

    private InvoiceStatus invoiceStatus;

    private LocalDateTime estimatedCompletionTime;

    private LocalDateTime requestTime;
}