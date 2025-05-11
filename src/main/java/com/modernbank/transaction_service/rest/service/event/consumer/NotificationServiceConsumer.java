package com.modernbank.transaction_service.rest.service.event.consumer;

import com.modernbank.transaction_service.api.client.NotificationServiceClient;
import com.modernbank.transaction_service.api.request.SendNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceConsumer {

    private final NotificationServiceClient notificationServiceClient;

    @KafkaListener(topics = "notification-service", groupId = "notification-service-group")
    public void consumeNotification(SendNotificationRequest request) {
        log.info("Received Notification by userid: " + request.getUserId());
        notificationServiceClient.sendNotification(request);
    }
}
