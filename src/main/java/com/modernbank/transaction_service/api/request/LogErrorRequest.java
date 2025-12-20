package com.modernbank.transaction_service.api.request;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogErrorRequest extends BaseRequest {
    private String traceId;       // Logları takip etmek için (MDC'den gelecek)

    private String requestPath;   // Hatanın olduğu endpoint (Örn: POST /api/v1/transfer)

    private String exceptionName; // Hatanın teknik adı (Örn: java.lang.NullPointerException)

    private String serviceName;   // Hangi servis? (transaction-service)

    private String errorCode;     // İş kodu (TRX-001) - Opsiyonel, yoksa null

    private String errorMessage;  // Exception.getMessage() içeriği

    private String stackTrace;    // Kısaltılmış stack trace

    private LocalDateTime timestamp;
}