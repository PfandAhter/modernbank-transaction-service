package com.modernbank.transaction_service.api.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DynamicInvoiceRequest {
    private String invoiceId;

    private String userId;
    private String invoiceType;

    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm:ss]")
    private LocalDateTime date;
    private Map<String,Object> data;
}