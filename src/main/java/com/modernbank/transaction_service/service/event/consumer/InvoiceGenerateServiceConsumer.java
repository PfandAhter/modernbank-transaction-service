package com.modernbank.transaction_service.service.event.consumer;

import com.modernbank.transaction_service.api.client.InvoiceServiceClient;
import com.modernbank.transaction_service.api.request.DynamicInvoiceRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceGenerateServiceConsumer {

    private final InvoiceServiceClient invoiceServiceClient;

    @KafkaListener(topics = "send-invoice-service", groupId = "send-invoice-group", containerFactory = "sendGenerateInvoiceKafkaListenerContainerFactory")
    public void consumeGenerateInvoice(DynamicInvoiceRequest request) {
        log.info("Received invoice generation request for this userId: " + request.getUserId());
        invoiceServiceClient.generateInvoice(request);
    }
}