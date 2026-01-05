package com.modernbank.transaction_service.api.request;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SendNotificationRequest{

    private String userId;

    private String message;

    private String type;

    private String title;

    private Map<String,Object> arguments;
}
