package com.modernbank.transaction_service.service.impl;

import com.modernbank.transaction_service.api.client.AccountServiceClient;
import com.modernbank.transaction_service.api.dto.AccountDTO;
import com.modernbank.transaction_service.api.request.AnalyzeTransactionRequest;
import com.modernbank.transaction_service.api.request.SendNotificationRequest;
import com.modernbank.transaction_service.api.response.GetAccountsResponse;
import com.modernbank.transaction_service.entity.FraudEvaluation;
import com.modernbank.transaction_service.entity.Transaction;
import com.modernbank.transaction_service.exception.NotFoundException;
import com.modernbank.transaction_service.model.DateRangeModel;
import com.modernbank.transaction_service.model.EnrichedTransaction;
import com.modernbank.transaction_service.model.TransactionAnalyzeModel;
import com.modernbank.transaction_service.model.enums.AnalyzeRange;
import com.modernbank.transaction_service.repository.FraudEvaluationRepository;
import com.modernbank.transaction_service.repository.TransactionRepository;
import com.modernbank.transaction_service.service.TechnicalErrorService;
import com.modernbank.transaction_service.service.TransactionAnalyzeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.modernbank.transaction_service.constant.ErrorCodeConstants.TECH_ACCOUNT_SERVICE_CLIENT_NOT_RESPONSE_ERROR;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionAnalyzeServiceImpl implements TransactionAnalyzeService {

    private final TransactionRepository transactionRepository;

    private final FraudEvaluationRepository fraudEvaluationRepository;

    private final KafkaTemplate<String, SendNotificationRequest> notificationKafkaTemplate;

    private final AccountServiceClient accountServiceClient;

    private final TechnicalErrorService technicalErrorService;

    @Override
    public TransactionAnalyzeModel analyzeUserTransactionHistory(AnalyzeTransactionRequest request) {
        GetAccountsResponse response = getAccounts(request.getUserId());
        String traceId = MDC.get("traceId");
        if (response == null) {
            String message = "İşleminiz sırasında sistemsel bir hata oluştu. Lütfen daha sonra tekrar deneyiniz.";
            sendSafeNotification(request.getUserId(), message, "ERROR", "Sistem Hatası", traceId);
            return TransactionAnalyzeModel.builder().transactions(List.of()).build();
        }
        DateRangeModel dateRange = getDateRange(request.getAnalyzeRange());

        List<String> accountIdList = response.getAccounts().stream()
                .map(AccountDTO::getId)
                .toList();

        List<Transaction> transactionList = transactionRepository.findTransactionsForAnalysis(
                        accountIdList,
                        dateRange.getStart(),
                        dateRange.getEnd())
                .orElseThrow(() -> new NotFoundException("Transaction History Not Found"));

        List<FraudEvaluation> frauds = fraudEvaluationRepository.findByTransactionIdIn(
                transactionList.stream().map(Transaction::getId).toList()
        );

        Map<String, FraudEvaluation> fraudMap = frauds.stream()
                .collect(Collectors.toMap(FraudEvaluation::getTransactionId, Function.identity(), (a, b) -> a));

        List<EnrichedTransaction> enrichedTransactions = transactionList.stream()
                .map(transaction -> {
                    EnrichedTransaction enriched = new EnrichedTransaction();
                    enriched.setTransaction(transaction);
                    enriched.setFraudEvaluation(fraudMap.get(transaction.getId()));
                    return enriched;
                })
                .toList();

        return TransactionAnalyzeModel.builder()
                .transactions(enrichedTransactions)
                .build();
    }

    private GetAccountsResponse getAccounts(String userId) {
        try {
            return accountServiceClient.getAccounts(userId);
        } catch (Exception exception) {
            log.error("Error at transfer finalize :  ", exception.getMessage());

            technicalErrorService.handleTechnicalError(
                    TECH_ACCOUNT_SERVICE_CLIENT_NOT_RESPONSE_ERROR,
                    exception
            );
            return null;
        }
    }

    private void sendSafeNotification(String userId, String message, String type, String title, String traceId) {
        try {
            ProducerRecord<String, SendNotificationRequest> record =
                    new ProducerRecord<>("notification-service", SendNotificationRequest.builder()
                            .type(type)
                            .title(title)
                            .userId(userId)
                            .message(message)
                            .build());
            record.headers().add("traceId", traceId.getBytes());

            notificationKafkaTemplate.send(record);
        } catch (Exception e) {
            log.warn("Failed to send notification for userId: {}. Error: {}", userId, e.getMessage());
        }
    }

    private DateRangeModel getDateRange(AnalyzeRange analyzeRange) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = switch (analyzeRange) {
            case LAST_7_DAYS -> end.minusDays(7);
            case LAST_30_DAYS -> end.minusDays(30);
        };

        return new DateRangeModel(start, end);
    }
}