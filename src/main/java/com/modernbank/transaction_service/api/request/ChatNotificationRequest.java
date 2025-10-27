package com.modernbank.transaction_service.api.request;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatNotificationRequest {
    private String message;
    private String type;
    private String title;
    private String userId;
    private String level;
    private Map<String, Object> arguments;
    private LocalDateTime time;
}