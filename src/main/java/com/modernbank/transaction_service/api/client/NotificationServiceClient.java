package com.modernbank.transaction_service.api.client;

import com.modernbank.transaction_service.api.request.ChatNotificationRequest;
import com.modernbank.transaction_service.api.request.SendNotificationRequest;
import com.modernbank.transaction_service.api.response.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "notification-service", url = "${feign.client.notification-service.url}")
public interface NotificationServiceClient {

    @PostMapping("${feign.client.notification-service.sendNotification}")
    BaseResponse sendNotification (@RequestBody SendNotificationRequest sendNotificationRequest);

    @PostMapping("${feign.client.notification-service.sendChatNotification}")
    BaseResponse sendChatNotification (@RequestBody ChatNotificationRequest chatNotificationRequest);
}