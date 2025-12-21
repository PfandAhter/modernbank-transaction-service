package com.modernbank.transaction_service.service.impl;


import com.modernbank.transaction_service.api.request.SendNotificationRequest;
import com.modernbank.transaction_service.api.request.TransferMoneyRequest;
import com.modernbank.transaction_service.entity.ErrorCodes;
import com.modernbank.transaction_service.model.TransactionErrorEvent;
import com.modernbank.transaction_service.model.enums.TransactionStatus;
import com.modernbank.transaction_service.model.enums.TransactionType;
import com.modernbank.transaction_service.repository.TransactionRepository;
import com.modernbank.transaction_service.service.ErrorCacheService;
import com.modernbank.transaction_service.service.TechnicalErrorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TechnicalErrorServiceImpl implements TechnicalErrorService {

    private final KafkaTemplate<String, SendNotificationRequest> notificationKafkaTemplate;
    private final KafkaTemplate<String, TransactionErrorEvent> errorEventKafkaTemplate;
    private final TransactionRepository transactionRepository;

    private final ErrorCacheService errorCacheService;

    @Override
    public void handleBusinessError(
            TransferMoneyRequest request,
            String transactionId,
            String userId,
            String errorCode,
            Object... args) {

        String userFriendlyMessage = resolveMessage(errorCode, args);

        log.warn("Business error occurred. Code: {}, Message: {}", errorCode, userFriendlyMessage);

        publishErrorEvent(TransactionErrorEvent.builder()
                .transactionId(transactionId)
                .userId(userId)
                .errorType("BUSINESS_ERROR")
                .errorCode(errorCode)
                .errorMessage(userFriendlyMessage)
                .transactionType(TransactionType.EXPENSE)
                .timestamp(LocalDateTime.now())
                .context(buildContextMap(request, args))
                .build());

        if (userId != null) {
            sendUserNotification(userId, "İşlem Tamamlanamadı", userFriendlyMessage, "WARNING");
        }

        if (transactionId != null) {
            updateTransactionStatus(transactionId, TransactionStatus.FAILED);
        }
    }

    @Override
    public void handleTechnicalError(
            TransferMoneyRequest request,
            String errorCode,
            Exception exception) {

        log.error("Technical error - code: {}", errorCode, exception);

        publishErrorEvent(TransactionErrorEvent.builder()
                .errorType("TECHNICAL_ERROR")
                .errorCode(errorCode)
                .errorMessage(exception.getMessage())
                .transactionType(TransactionType.EXPENSE)
                .timestamp(LocalDateTime.now())
                .context(Map.of(
                        "fromIBAN", request != null ? request.getFromIBAN() : "UNKNOWN",
                        "exceptionType", exception.getClass().getSimpleName()
                ))
                .build());

        // Teknik hatada kullanıcıya bildirim gitmeli mi? Proje kararıdır.
        // Gidecekse "Sistemsel bir hata oluştu" şeklinde gitmeli.
    }

    private String resolveMessage(String errorCode, Object[] args) {
        try {
            ErrorCodes errorData = errorCacheService.getErrorCodeByErrorId(errorCode);

            if (errorData == null) {
                return "İşlem sırasında beklenmedik bir durum oluştu.";
            }

            String template = errorData.getDescription();

            if (args != null && args.length > 0) {
                return java.text.MessageFormat.format(template, args);
            }

            return template;
        } catch (Exception e) {
            log.error("Error resolving message for code: {}", errorCode, e);
            return "İşleminiz gerçekleştirilemedi.";
        }
    }

    private Map<String, Object> buildContextMap(TransferMoneyRequest request, Object[] args) {
        Map<String, Object> context = new HashMap<>();
        if(request != null) {
            context.put("fromIBAN", request.getFromIBAN());
            context.put("toIBAN", request.getToIBAN());
            context.put("amount", request.getAmount());
        }
        if (args != null) {
            context.put("errorArgs", Arrays.toString(args));
        }
        return context;
    }

    private void sendUserNotification(String userId, String title, String message, String type) {
        try {
            notificationKafkaTemplate.send("notification-service",
                    SendNotificationRequest.builder()
                            .userId(userId)
                            .title(title)
                            .message(message)
                            .type(type)
                            .build());
        } catch (Exception e) {
            log.error("Failed to send notification to user: {}", userId, e);
        }
    }

    private void publishErrorEvent(TransactionErrorEvent event) {
        try {
            errorEventKafkaTemplate.send("transaction-errors", event);
        } catch (Exception e) {
            log.error("Failed to publish error event", e);
        }
    }

    private void updateTransactionStatus(String transactionId, TransactionStatus status) {
        try {
            transactionRepository.findById(transactionId).ifPresent(transaction -> {
                transaction.setStatus(status);
                transaction.setUpdatedDate(LocalDateTime.now());
                transactionRepository.save(transaction);
            });
        } catch (Exception e) {
            log.error("Failed to update transaction status: {}", transactionId, e);
        }
    }
}