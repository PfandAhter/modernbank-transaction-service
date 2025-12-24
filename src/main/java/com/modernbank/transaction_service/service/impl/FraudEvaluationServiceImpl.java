package com.modernbank.transaction_service.service.impl;

import com.modernbank.transaction_service.api.client.AccountServiceClient;
import com.modernbank.transaction_service.api.request.FraudCheckRequest;
import com.modernbank.transaction_service.api.response.AccountProfileResponse;
import com.modernbank.transaction_service.api.response.FraudCheckResponse;
import com.modernbank.transaction_service.entity.FraudEvaluation;
import com.modernbank.transaction_service.entity.Transaction;
import com.modernbank.transaction_service.model.enums.FraudDecision;
import com.modernbank.transaction_service.model.enums.FraudDecisionAction;
import com.modernbank.transaction_service.model.enums.RiskLevel;
import com.modernbank.transaction_service.repository.FraudEvaluationRepository;
import com.modernbank.transaction_service.repository.TransactionRepository;
import com.modernbank.transaction_service.service.FraudEvaluationService;
import com.modernbank.transaction_service.service.util.FeatureVectorSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudEvaluationServiceImpl implements FraudEvaluationService {

    private final ResilientFraudMLService resilientFraudMLService;

    private final AccountServiceClient accountServiceClient;

    private final TransactionRepository transactionRepository;

    private final FraudEvaluationRepository fraudEvaluationRepository;

    private final FeatureVectorSerializer featureVectorSerializer;

    @Override
    public FraudDecision evaluateAndDecide(Transaction transaction, String senderAccountId) {
        log.info("Evaluating fraud for transaction: accountId={}, amount={}",
                senderAccountId, transaction.getAmount());
        try {
            FraudCheckRequest request = buildFraudCheckRequest(transaction, senderAccountId);

            FraudCheckResponse response = resilientFraudMLService.evaluateTransaction(request);
            createFraudEvaluation(request, response);

            RiskLevel riskLevel = mapToRiskLevel(response.getRiskLevel());
            FraudDecision decision = FraudDecision.fromRiskLevel(riskLevel);

            updateTransactionWithFraudResults(transaction, response, riskLevel, decision);

            log.info("Fraud evaluation complete: transactionId={}, riskScore={}, riskLevel={}, decision={}",
                    transaction.getId(), response.getRiskScore(), riskLevel, decision);

            return decision;

        } catch (Exception e) {
            log.error("Error during fraud evaluation: {}", e.getMessage(), e);
            updateTransactionWithFallback(transaction);
            return FraudDecision.APPROVE;
        }
    }

    private void createFraudEvaluation(FraudCheckRequest request, FraudCheckResponse response) {
        fraudEvaluationRepository.save(FraudEvaluation.builder()
                .transactionId(request.getTransactionId())
                .userId(request.getUserId())
                .riskScore(response.getRiskScore())
                .riskLevel(RiskLevel.valueOf(response.getRiskLevel()))
                .recommendedAction(FraudDecisionAction.valueOf(response.getRecommendedAction()))
                .featureVector(featureVectorSerializer.toJson(buildFraudVector(request)))
                .featureImportance(featureVectorSerializer.toJson(response.getFeatureImportance()))
                .modelVersion(response.getModelVersion())
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    public boolean isAccountBlocked(String accountId) {
        try {
            Boolean blocked = accountServiceClient.isAccountBlocked(accountId);
            if (Boolean.TRUE.equals(blocked)) {
                log.warn("Account {} is temporarily blocked", accountId);
            }
            return Boolean.TRUE.equals(blocked);
        } catch (Exception e) {
            log.warn("Failed to check account block status: {}, assuming not blocked", e.getMessage());
            return false;
        }
    }

    @Override
    public void incrementFraudCounter(String accountId, String reason) {
        try {
            accountServiceClient.confirmFraud(accountId, reason);
            log.info("Incremented fraud counter for accountId: {}", accountId);
        } catch (Exception e) {
            log.error("Failed to increment fraud counter for accountId {}: {}", accountId, e.getMessage());
        }
    }

    private FraudCheckRequest buildFraudCheckRequest(Transaction transaction, String senderAccountId) {
        AccountProfileResponse profile = null;
        try {
            profile = accountServiceClient.getAccountProfileByAccountId(senderAccountId);
        } catch (Exception e) {
            log.warn("Could not fetch account profile: {}", e.getMessage());
        }

        Double avgTransactionAmount7d = transactionRepository.findAvgAmountSinceBySender(
                senderAccountId, LocalDateTime.now().minusDays(7)
        );

        Integer transactionCount24h = transactionRepository.countByAccountIdAndDateAfter(
                senderAccountId, LocalDateTime.now().minusHours(24)
        );

        Integer transactionCount7d = transactionRepository.countByAccountIdAndDateBetween(
                senderAccountId, LocalDateTime.now().minusDays(7), LocalDateTime.now()
        );

        boolean isNewReceiver = !transactionRepository.existsByAccountIdAndReceiverIbanAndDateBefore(
                senderAccountId, transaction.getReceiverIban(), LocalDateTime.now().minusDays(30)
        );

        LocalDateTime eventTs = transaction.getDate() != null ? transaction.getDate() : LocalDateTime.now();
        boolean isWeekend = eventTs.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
                || eventTs.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;

        FraudCheckRequest.FraudCheckRequestBuilder builder = FraudCheckRequest.builder()
                .transactionId(transaction.getId())
                .userId(profile.getUserId())
                .transactionAmount(transaction.getAmount())
                .transactionType(transaction.getCategory().getCategory())
                .merchantCategory(transaction.getChannel().getChannel())
                .cardType(profile.getCardType())
                .cardAgeMonths(profile.getCardAgeMonths())
                .accountBalanceBefore(profile.getCurrentBalance())
                .avgTransactionAmount7d(avgTransactionAmount7d)
                .transactionCount24h(transactionCount24h)
                .transactionCount7d(transactionCount7d)
                .previousFraudFlag(profile.getPreviousFraudFlag())
                .isNewReceiver(isNewReceiver)
                .isWeekend(isWeekend)
                .timestamp(LocalDateTime.now());

        if (profile.getCurrentBalance() != null && profile.getCurrentBalance() > 0) {
            builder.amountToBalanceRatio(transaction.getAmount() / profile.getCurrentBalance());
        } else {
            builder.amountToBalanceRatio(1.0);
        }

        return builder.build();
    }

    private RiskLevel mapToRiskLevel(String riskLevelStr) {
        if (riskLevelStr == null) {
            return RiskLevel.LOW;
        }
        try {
            return RiskLevel.valueOf(riskLevelStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown risk level: {}, defaulting to LOW", riskLevelStr);
            return RiskLevel.LOW;
        }
    }

    private void updateTransactionWithFraudResults(
            Transaction transaction,
            FraudCheckResponse response,
            RiskLevel riskLevel,
            FraudDecision decision) {
        transaction.setRiskScore(response.getRiskScore());
        transaction.setRiskLevel(riskLevel);
        transaction.setFraudDecision(decision);
        transaction.setFraudEvaluatedAt(LocalDateTime.now());

        // Set decision reason based on risk level
        String reason = switch (decision) {
            case APPROVE -> "Risk level LOW - transaction approved";
            case HOLD -> "Risk level MEDIUM - awaiting user confirmation";
            case BLOCK -> "Risk level HIGH - transaction rejected";
        };
        transaction.setFraudDecisionReason(reason);
    }

    private void updateTransactionWithFallback(Transaction transaction) {
        transaction.setRiskScore(0.0);
        transaction.setRiskLevel(RiskLevel.LOW);
        transaction.setFraudDecision(FraudDecision.APPROVE);
        transaction.setFraudEvaluatedAt(LocalDateTime.now());
        transaction.setFraudDecisionReason("Fallback - fraud service unavailable");
    }

    public static Map<String, Double> buildFraudVector(FraudCheckRequest r) {
        Map<String, Double> features = new HashMap<>();

        features.put("amountToAvgRatio",
                safeDivide(r.getTransactionAmount(), r.getAvgTransactionAmount7d()));

        features.put("balanceDrainRatio",
                safeDivide(r.getTransactionAmount(), r.getAccountBalanceBefore()));

        features.put("velocity24h", r.getTransactionCount24h().doubleValue());
        features.put("velocity7d", r.getTransactionCount7d().doubleValue());

        features.put("cardAgeMonths", r.getCardAgeMonths().doubleValue());

        features.put("isNewReceiver", bool(r.getIsNewReceiver()));
        features.put("isWeekend", bool(r.getIsWeekend()));
        features.put("previousFraudFlag", bool(r.getPreviousFraudFlag()));

        features.put("isOffHours", isOffHours(r.getTimestamp()));

        return features;
    }

    private static double safeDivide(Double a, Double b) {
        if (a == null || b == null || b == 0) return 0.0;
        return a / b;
    }

    private static double bool(Boolean b) {
        return Boolean.TRUE.equals(b) ? 1.0 : 0.0;
    }

    private static double isOffHours(LocalDateTime t) {
        int h = t.getHour();
        return (h < 6 || h > 22) ? 1.0 : 0.0;
    }
}