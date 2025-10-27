package com.modernbank.transaction_service.api.request;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LogErrorRequest extends BaseRequest {
    private String serviceName;
    private String errorCode;
    private String exceptionName;
    private String timestamp;
    private String stackTrace;
}