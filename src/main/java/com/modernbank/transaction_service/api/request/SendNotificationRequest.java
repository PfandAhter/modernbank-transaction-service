package com.modernbank.transaction_service.api.request;

import lombok.*;

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
}
