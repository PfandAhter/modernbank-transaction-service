package com.modernbank.transaction_service.service.event.consumer;

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

    @KafkaListener(topics = "notification-service", groupId = "notification-service-group", containerFactory = "notificationKafkaListenerContainerFactory")
    public void consumeNotification(SendNotificationRequest request) {
        try{
            log.info("Received Notification by userid: " + request.getUserId());
            notificationServiceClient.sendNotification(request);
        }catch(Exception e){
            log.error("Error: {} while sending notification by userId {}: ",e.getMessage(),request.getUserId());
        }

    }
}
