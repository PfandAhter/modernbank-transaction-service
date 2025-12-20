package com.modernbank.transaction_service.service.event.consumer;

import com.modernbank.transaction_service.api.client.ParameterServiceClient;
import com.modernbank.transaction_service.api.request.LogErrorRequest;
import com.modernbank.transaction_service.model.TransactionErrorEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorMonitoringConsumer {

    private final ParameterServiceClient parameterServiceClient;
    //private final MetricsService metricsService; // Prometheus/Grafana için

    @KafkaListener(topics = "transaction-errors", groupId = "error-monitoring-group")
    public void handleErrorEvent(TransactionErrorEvent event) {
        log.info("Processing error event: {}", event);
        String traceId = MDC.get("traceId");

        try {
            LogErrorRequest logErrorRequest = LogErrorRequest.builder()
                    .traceId(traceId)
                    .requestPath("POST /api/v1/transfer")
                    .exceptionName(event.getErrorType())
                    .serviceName("transaction-service")
                    .errorCode(event.getErrorCode())
                    .errorMessage(event.getErrorMessage())
                    .stackTrace("No stack trace available.")
                    .timestamp(event.getTimestamp())
                    .build();
            logErrorRequest.setUserId(event.getUserId());

            parameterServiceClient.logError(logErrorRequest);
        } catch (Exception e) {
            log.error("Failed to log error to parameter service", e);
        }
//        metricsService.incrementErrorCounter(event.getErrorCode());

        if (isCriticalError(event.getErrorType())) {
            sendAlertToOpsTeam(event);
        }
    }

    private boolean isCriticalError(String errorCode) {
        return List.of("TECHNICAL_ERROR", "SERVICE_UNAVAILABLE", "ROLLBACK_FAILED")
                .contains(errorCode);
    }

    private void sendAlertToOpsTeam(TransactionErrorEvent event) {
        log.error("CRITICAL ERROR ALERT: {}", event);
    }
}