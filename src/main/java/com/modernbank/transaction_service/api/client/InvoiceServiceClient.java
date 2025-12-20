package com.modernbank.transaction_service.api.client;

import com.modernbank.transaction_service.api.request.DynamicInvoiceRequest;
import com.modernbank.transaction_service.api.response.CreateInvoicePDFResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "invoice-service", url = "${feign.client.invoice-service.url}")
public interface InvoiceServiceClient {

    @PostMapping("${feign.client.invoice-service.generateInvoice}")
    CreateInvoicePDFResponse generateInvoice(@RequestBody DynamicInvoiceRequest request);
}