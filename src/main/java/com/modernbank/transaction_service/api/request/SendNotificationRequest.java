package com.modernbank.transaction_service.api.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SendNotificationRequest {
    private String userId;

    private String message;
}
