package com.modernbank.transaction_service.api.request;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatNotificationRequest extends BaseRequest {
    private String message;
    private String type;
    private String title;
    private Map<String, Object> arguments;
    private LocalDateTime time;
}