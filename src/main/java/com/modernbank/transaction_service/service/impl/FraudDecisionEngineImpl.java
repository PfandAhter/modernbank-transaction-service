package com.modernbank.transaction_service.service.impl;

import com.modernbank.transaction_service.api.client.AccountServiceClient;
import com.modernbank.transaction_service.api.response.AccountProfileResponse;
import com.modernbank.transaction_service.api.response.FraudCheckResponse;
import com.modernbank.transaction_service.entity.FraudDecision;
import com.modernbank.transaction_service.entity.PendingTransaction;
import com.modernbank.transaction_service.model.enums.FraudDecisionAction;
import com.modernbank.transaction_service.model.enums.RiskLevel;
import com.modernbank.transaction_service.model.enums.TransactionStatus;
import com.modernbank.transaction_service.repository.FraudDecisionRepository;
import com.modernbank.transaction_service.repository.PendingTransactionRepository;
import com.modernbank.transaction_service.service.FraudDecisionEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementation of the Fraud Decision Engine.
 * 
 * NON-NEGOTIABLE RULES:
 * - Never auto-block on single ML output
 * - Never mutate account data directly (read-only from Account Service)
 * - Never let funds move on unresolved risk
 * - Always keep decisions explainable
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDecisionEngineImpl implements FraudDecisionEngine {

    private final FraudDecisionRepository fraudDecisionRepository;
    private final PendingTransactionRepository pendingTransactionRepository;
    private final AccountServiceClient accountServiceClient;

    @Value("${fraud.decision.hold-timeout-minutes:15}")
    private int holdTimeoutMinutes;

    @Value("${fraud.decision.strong-auth-timeout-minutes:5}")
    private int strongAuthTimeoutMinutes;

    @Value("${fraud.risk.low-threshold:0.30}")
    private double lowThreshold;

    @Value("${fraud.risk.high-threshold:0.70}")
    private double highThreshold;

    @Override
    @Transactional
    public FraudDecision evaluate(
            FraudCheckResponse mlResponse,
            AccountProfileResponse accountProfile,
            String receiverIban,
            String pendingTransactionId,
            Double amount) {
        log.info("Evaluating fraud decision for pendingTransactionId: {}, riskScore: {}",
                pendingTransactionId, mlResponse.getRiskScore());

        // Classify risk level
        RiskLevel riskLevel = classifyRisk(mlResponse.getRiskScore());

        // Calculate amount to balance ratio (for credit scoring)
        Double amountToBalanceRatio = calculateAmountToBalanceRatio(amount, accountProfile.getCurrentBalance());

        // Check if receiver is new
        Boolean isNewReceiver = checkIsNewReceiver(accountProfile.getAccountId(), receiverIban);

        // Count high risk transactions in last 24 hours
        int highRiskCount24h = countHighRiskIn24Hours(accountProfile.getUserId());

        // Determine action based on business rules
        FraudDecisionAction action = determineAction(
                riskLevel,
                accountProfile,
                receiverIban,
                highRiskCount24h);

        // Build decision reason
        String decisionReason = buildDecisionReason(action, riskLevel, highRiskCount24h, accountProfile);

        // Calculate expiry time based on action
        LocalDateTime expiresAt = calculateExpiryTime(action);

        // Create and save fraud decision
        FraudDecision fraudDecision = FraudDecision.builder()
                .pendingTransactionId(pendingTransactionId)
                .accountId(accountProfile.getAccountId())
                .userId(accountProfile.getUserId())
                .senderIban(accountProfile.getIban())
                .receiverIban(receiverIban)
                .amount(amount)
                .riskScore(mlResponse.getRiskScore())
                .riskLevel(riskLevel)
                .amountToBalanceRatio(amountToBalanceRatio)
                .newReceiverFlag(isNewReceiver)
                .decisionTaken(action)
                .mlRecommendedAction(mlResponse.getRecommendedAction())
                .decisionReason(decisionReason)
                .highRiskCount24h(highRiskCount24h)
                .expiresAt(expiresAt)
                .build();

        fraudDecision = fraudDecisionRepository.save(fraudDecision);

        log.info("Fraud decision made: pendingTransactionId={}, action={}, reason={}",
                pendingTransactionId, action, decisionReason);

        return fraudDecision;
    }

    /**
     * DECISION MATRIX (MANDATORY):
     * 
     * | Condition | Action |
     * |----------------------------------|-----------------|
     * | LOW | APPROVE |
     * | MEDIUM | HOLD + Notify |
     * | HIGH (first occurrence) | HOLD_STRONG_AUTH|
     * | HIGH (≥2 in 24h) | BLOCK |
     * | HIGH + previous confirmed fraud | BLOCK |
     * | Known blacklisted receiver | BLOCK |
     */
    private FraudDecisionAction determineAction(
            RiskLevel riskLevel,
            AccountProfileResponse accountProfile,
            String receiverIban,
            int highRiskCount24h) {
        // Rule: Known blacklisted receiver → BLOCK
        if (isBlacklisted(receiverIban)) {
            log.warn("Receiver IBAN {} is blacklisted", receiverIban);
            return FraudDecisionAction.BLOCK;
        }

        // Rule: LOW risk → APPROVE
        if (riskLevel == RiskLevel.LOW) {
            return FraudDecisionAction.APPROVE;
        }

        // Rule: MEDIUM risk → HOLD + Notify
        if (riskLevel == RiskLevel.MEDIUM) {
            return FraudDecisionAction.HOLD;
        }

        // HIGH risk cases

        // Rule: HIGH + previous confirmed fraud → BLOCK
        if (accountProfile.getPreviousFraudCount() != null && accountProfile.getPreviousFraudCount() > 0) {
            log.warn("User {} has previous confirmed fraud, blocking", accountProfile.getUserId());
            return FraudDecisionAction.BLOCK;
        }

        // Rule: HIGH (≥2 in 24h) → BLOCK
        if (highRiskCount24h >= 2) {
            log.warn("User {} has {} HIGH risk transactions in 24h, blocking",
                    accountProfile.getUserId(), highRiskCount24h);
            return FraudDecisionAction.BLOCK;
        }

        // Rule: HIGH (first occurrence) → HOLD_STRONG_AUTH
        return FraudDecisionAction.HOLD_STRONG_AUTH;
    }

    private RiskLevel classifyRisk(Double score) {
        if (score == null) {
            log.warn("Risk score is null, defaulting to MEDIUM");
            return RiskLevel.MEDIUM;
        }
        if (score < lowThreshold)
            return RiskLevel.LOW;
        if (score <= highThreshold)
            return RiskLevel.MEDIUM;
        return RiskLevel.HIGH;
    }

    private Double calculateAmountToBalanceRatio(Double amount, Double currentBalance) {
        if (currentBalance == null || currentBalance <= 0) {
            return 1.0; // Max ratio if balance is zero or negative
        }
        return amount / currentBalance;
    }

    private Boolean checkIsNewReceiver(String senderAccountId, String receiverIban) {
        // TODO: Implement by checking transaction history
        // For now, return false (not new)
        return false;
    }

    private int countHighRiskIn24Hours(String userId) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return fraudDecisionRepository.countByUserIdAndRiskLevelSince(
                userId, RiskLevel.HIGH, since);
    }

    private boolean isBlacklisted(String receiverIban) {
        try {
            Boolean result = accountServiceClient.isReceiverBlacklisted(receiverIban);
            return result != null && result;
        } catch (Exception e) {
            log.warn("Failed to check blacklist for IBAN {}: {}", receiverIban, e.getMessage());
            return false; // Fail-open for now, but log for review
        }
    }

    private String buildDecisionReason(
            FraudDecisionAction action,
            RiskLevel riskLevel,
            int highRiskCount24h,
            AccountProfileResponse profile) {
        return switch (action) {
            case APPROVE -> String.format("Risk level %s below threshold", riskLevel);
            case HOLD -> String.format("Medium risk (%.2f threshold), awaiting user confirmation", highThreshold);
            case HOLD_STRONG_AUTH -> String.format("High risk first occurrence, requiring strong authentication");
            case BLOCK -> {
                if (profile.getPreviousFraudCount() != null && profile.getPreviousFraudCount() > 0) {
                    yield String.format("Previous confirmed fraud count: %d", profile.getPreviousFraudCount());
                } else if (highRiskCount24h >= 2) {
                    yield String.format("%d HIGH risk transactions in 24 hours", highRiskCount24h);
                } else {
                    yield "Blacklisted receiver or pattern detected";
                }
            }
        };
    }

    private LocalDateTime calculateExpiryTime(FraudDecisionAction action) {
        return switch (action) {
            case APPROVE -> null; // No expiry for approved
            case HOLD -> LocalDateTime.now().plusMinutes(holdTimeoutMinutes);
            case HOLD_STRONG_AUTH -> LocalDateTime.now().plusMinutes(strongAuthTimeoutMinutes);
            case BLOCK -> null; // No expiry for blocked
        };
    }

    @Override
    @Transactional
    public boolean confirmHoldTransaction(String pendingTransactionId, String confirmationType) {
        log.info("Confirming HOLD transaction: {}, type: {}", pendingTransactionId, confirmationType);

        FraudDecision decision = fraudDecisionRepository.findByPendingTransactionId(pendingTransactionId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Fraud decision not found for: " + pendingTransactionId));

        // Check if not expired
        if (decision.getExpiresAt() != null && decision.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Transaction {} has expired", pendingTransactionId);
            return false;
        }

        // Update decision
        decision.setConfirmationResult(confirmationType);
        decision.setDecidedAt(LocalDateTime.now());
        decision.setTimeToConfirm(
                java.time.Duration.between(decision.getCreatedAt(), LocalDateTime.now()).toMillis());
        fraudDecisionRepository.save(decision);

        // Update pending transaction status
        PendingTransaction pending = pendingTransactionRepository.findById(pendingTransactionId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Pending transaction not found: " + pendingTransactionId));
        pending.setStatus(TransactionStatus.PENDING);
        pending.setCurrentStage("CONFIRMED");
        pendingTransactionRepository.save(pending);

        return true;
    }

    @Override
    @Transactional
    public void timeoutHoldTransaction(String pendingTransactionId) {
        log.info("Timing out HOLD transaction: {}", pendingTransactionId);

        FraudDecision decision = fraudDecisionRepository.findByPendingTransactionId(pendingTransactionId)
                .orElse(null);

        if (decision != null) {
            decision.setConfirmationResult("TIMEOUT");
            decision.setDecidedAt(LocalDateTime.now());
            fraudDecisionRepository.save(decision);
        }

        PendingTransaction pending = pendingTransactionRepository.findById(pendingTransactionId)
                .orElse(null);

        if (pending != null) {
            pending.setStatus(TransactionStatus.CANCELLED);
            pending.setCurrentStage("TIMEOUT_CANCELLED");
            pending.setLastError("User confirmation timeout");
            pendingTransactionRepository.save(pending);
        }
    }
}
