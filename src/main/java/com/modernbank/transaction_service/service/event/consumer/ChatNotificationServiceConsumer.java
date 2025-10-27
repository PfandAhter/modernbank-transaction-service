package com.modernbank.transaction_service.service.event.consumer;

import com.modernbank.transaction_service.api.client.NotificationServiceClient;
import com.modernbank.transaction_service.api.request.ChatNotificationRequest;
import com.modernbank.transaction_service.exception.ProcessFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatNotificationServiceConsumer {
    private final NotificationServiceClient notificationServiceClient;

    @KafkaListener(topics = "chat-notification-service",
            groupId = "chat-notification-service-group",
            containerFactory = "sendChatNotificationKafkaListenerContainerFactory")
    public void consumeChatNotification(ChatNotificationRequest request) {
        try {
            log.info("Received Chat Notification by userid: {}", request.getUserId());
            notificationServiceClient.sendChatNotification(request);
        } catch (Exception e) {
            log.error("Error processing chat notification: {}", e.getMessage());
            throw new ProcessFailedException("Failed to process chat notification: " + e.getMessage());
        }

    }
}