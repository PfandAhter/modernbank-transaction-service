package com.modernbank.transaction_service.service.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.modernbank.transaction_service.api.client.AccountServiceClient;
import com.modernbank.transaction_service.api.request.SendNotificationRequest;
import com.modernbank.transaction_service.api.request.TransferMoneyRequest;
import com.modernbank.transaction_service.entity.FraudDecision;
import com.modernbank.transaction_service.entity.PendingTransaction;
import com.modernbank.transaction_service.model.enums.TransactionStatus;
import com.modernbank.transaction_service.repository.FraudDecisionRepository;
import com.modernbank.transaction_service.repository.PendingTransactionRepository;
import com.modernbank.transaction_service.service.FraudDecisionEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job for transaction recovery and timeout handling.
 * 
 * Responsibilities:
 * - Handle expired HOLD transactions (auto-cancel after timeout)
 * - Handle expired strong auth requests
 * - Recover stuck transactions
 * - Release held balances when needed
 * 
 * TIMEOUT POLICY:
 * - User confirmation: 15 minutes
 * - Strong auth (OTP): 5 minutes
 * - No response: Auto-cancel
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionRecoveryJob {

    private final PendingTransactionRepository pendingTransactionRepository;
    private final FraudDecisionRepository fraudDecisionRepository;
    private final FraudDecisionEngine fraudDecisionEngine;
    private final AccountServiceClient accountServiceClient;
    private final KafkaTemplate<String, SendNotificationRequest> notificationKafkaTemplate;
    private final KafkaTemplate<String, TransferMoneyRequest> transferMoneyKafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${fraud.recovery.stuck-threshold-minutes:30}")
    private int stuckThresholdMinutes;

    /**
     * Handle expired HOLD transactions.
     * Runs every minute.
     */
    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void handleExpiredHolds() {
        log.debug("Running expired holds check...");

        List<PendingTransaction> expiredHolds = pendingTransactionRepository
                .findByStatusAndHoldExpiresAtBefore(TransactionStatus.HOLD, LocalDateTime.now());

        for (PendingTransaction pending : expiredHolds) {
            try {
                handleExpiredHold(pending);
            } catch (Exception e) {
                log.error("Failed to handle expired hold for transaction {}: {}",
                        pending.getId(), e.getMessage());
            }
        }

        if (!expiredHolds.isEmpty()) {
            log.info("Processed {} expired HOLD transactions", expiredHolds.size());
        }
    }

    private void handleExpiredHold(PendingTransaction pending) {
        log.info("Handling expired HOLD transaction: {}", pending.getId());

        // 1. Update fraud decision
        fraudDecisionEngine.timeoutHoldTransaction(pending.getId());

        // 2. Cancel the transaction
        pending.setStatus(TransactionStatus.CANCELLED);
        pending.setCurrentStage("TIMEOUT_CANCELLED");
        pending.setLastError("User confirmation timeout");
        pendingTransactionRepository.save(pending);

        // 3. Release any held balance (if debited)
        if (Boolean.TRUE.equals(pending.getBalanceDebited()) && pending.getDebitedAmount() != null) {
            try {
                accountServiceClient.updateBalance(
                        pending.getSenderIban(),
                        pending.getDebitedAmount() // Positive amount to credit back
                );
                log.info("Released held balance {} for transaction {}",
                        pending.getDebitedAmount(), pending.getId());
            } catch (Exception e) {
                log.error("Failed to release balance for transaction {}: {}",
                        pending.getId(), e.getMessage());
            }
        }

        // 4. Notify user (without mentioning fraud)
        sendNotification(
                pending.getSenderUserId(),
                "İşlem İptal Edildi",
                "Güvenlik doğrulaması için belirlenen süre dolduğundan işleminiz iptal edilmiştir.",
                "INFO");

        log.info("Cancelled expired HOLD transaction: {}", pending.getId());
    }

    /**
     * Handle expired strong auth requests.
     * Runs every 30 seconds.
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    @Transactional
    public void handleExpiredStrongAuth() {
        log.debug("Running expired strong auth check...");

        List<PendingTransaction> expiredAuth = pendingTransactionRepository
                .findExpiredStrongAuthTransactions(TransactionStatus.HOLD, LocalDateTime.now());

        for (PendingTransaction pending : expiredAuth) {
            try {
                handleExpiredStrongAuthTransaction(pending);
            } catch (Exception e) {
                log.error("Failed to handle expired strong auth for transaction {}: {}",
                        pending.getId(), e.getMessage());
            }
        }
    }

    private void handleExpiredStrongAuthTransaction(PendingTransaction pending) {
        log.info("Handling expired strong auth for transaction: {}", pending.getId());

        // Clear the auth code
        pending.setAuthCode(null);
        pending.setAuthCodeExpiresAt(null);
        pending.setLastError("OTP verification timeout");

        // Check if we should cancel or just reset the auth
        // For now, we cancel after OTP timeout
        pending.setStatus(TransactionStatus.CANCELLED);
        pending.setCurrentStage("AUTH_TIMEOUT");
        pendingTransactionRepository.save(pending);

        // Update fraud decision
        FraudDecision decision = fraudDecisionRepository
                .findByPendingTransactionId(pending.getId())
                .orElse(null);
        if (decision != null) {
            decision.setConfirmationResult("AUTH_TIMEOUT");
            decision.setDecidedAt(LocalDateTime.now());
            fraudDecisionRepository.save(decision);
        }

        // Notify user
        sendNotification(
                pending.getSenderUserId(),
                "Doğrulama Süresi Doldu",
                "Doğrulama kodu için belirlenen süre dolmuştur. Lütfen işlemi tekrar başlatın.",
                "WARNING");
    }

    /**
     * Recover stuck transactions.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void recoverStuckTransactions() {
        log.debug("Running stuck transaction recovery...");

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(stuckThresholdMinutes);

        // Find transactions stuck in FRAUD_REVIEW stage
        List<PendingTransaction> stuckInFraudReview = pendingTransactionRepository
                .findStuckTransactions("FRAUD_EVALUATE", threshold);

        for (PendingTransaction pending : stuckInFraudReview) {
            try {
                recoverStuckTransaction(pending, "FRAUD_EVALUATE");
            } catch (Exception e) {
                log.error("Failed to recover stuck transaction {}: {}",
                        pending.getId(), e.getMessage());
            }
        }

        // Find transactions stuck in INITIATED stage
        List<PendingTransaction> stuckInInitiated = pendingTransactionRepository
                .findStuckTransactions("INITIATED", threshold);

        for (PendingTransaction pending : stuckInInitiated) {
            try {
                recoverStuckTransaction(pending, "INITIATED");
            } catch (Exception e) {
                log.error("Failed to recover stuck transaction {}: {}",
                        pending.getId(), e.getMessage());
            }
        }

        int totalRecovered = stuckInFraudReview.size() + stuckInInitiated.size();
        if (totalRecovered > 0) {
            log.info("Recovered {} stuck transactions", totalRecovered);
        }
    }

    private void recoverStuckTransaction(PendingTransaction pending, String stage) {
        log.warn("Recovering stuck transaction: id={}, stage={}", pending.getId(), stage);

        // Increment retry count
        pending.setRetriesCount(pending.getRetriesCount() + 1);

        // If max retries exceeded, cancel
        if (pending.getRetriesCount() >= 3) {
            log.error("Max retries exceeded for transaction {}, cancelling", pending.getId());
            pending.setStatus(TransactionStatus.FAILED);
            pending.setCurrentStage("MAX_RETRIES_EXCEEDED");
            pending.setLastError("Transaction recovery failed after " + pending.getRetriesCount() + " retries");
            pendingTransactionRepository.save(pending);

            // Release balance if needed
            if (Boolean.TRUE.equals(pending.getBalanceDebited())) {
                try {
                    accountServiceClient.updateBalance(
                            pending.getSenderIban(),
                            pending.getDebitedAmount());
                } catch (Exception e) {
                    log.error("Failed to release balance during recovery: {}", e.getMessage());
                }
            }

            // Notify user
            sendNotification(
                    pending.getSenderUserId(),
                    "İşlem Başarısız",
                    "Transfer işleminiz teknik bir sorun nedeniyle tamamlanamadı. Lütfen tekrar deneyin.",
                    "ERROR");
            return;
        }

        // Update last error with recovery info
        pending.setLastError("Recovered from stuck state at " + LocalDateTime.now());
        pendingTransactionRepository.save(pending);

        // Re-send to appropriate Kafka topic for retry
        try {
            retryTransaction(pending, stage);
        } catch (Exception e) {
            log.error("Failed to retry transaction {}: {}", pending.getId(), e.getMessage());
            pending.setLastError("Retry failed: " + e.getMessage());
            pendingTransactionRepository.save(pending);
        }
    }

    /**
     * Re-sends a stuck transaction to the appropriate Kafka topic for retry.
     * Uses the serialized original request stored in the pending transaction.
     */
    private void retryTransaction(PendingTransaction pending, String stage)
            throws JsonProcessingException {

        TransferMoneyRequest request;

        // Check if we have the original request JSON
        if (pending.getOriginalRequestJson() == null || pending.getOriginalRequestJson().isEmpty()) {
            log.warn("Original request JSON missing for transaction {}, reconstructing from entity",
                    pending.getId());

            // Fallback: reconstruct request from pending transaction fields
            request = TransferMoneyRequest.builder()
                    .fromIBAN(pending.getSenderIban())
                    .toIBAN(pending.getReceiverIban())
                    .amount(pending.getAmount())
                    .description(pending.getDescription())
                    .toFirstName(pending.getReceiverFirstName())
                    .toSecondName(pending.getReceiverSecondName())
                    .toLastName(pending.getReceiverLastName())
                    .byAi(pending.getByAi())
                    .isConfirmed(true) // Skip validation on retry
                    .build();
        } else {
            // Deserialize the original request
            request = objectMapper.readValue(
                    pending.getOriginalRequestJson(),
                    TransferMoneyRequest.class);
            // Ensure isConfirmed is true for retry (skip validation)
            request.setIsConfirmed(true);
        }

        sendToKafkaTopic(pending, request, stage);
    }

    /**
     * Sends the transaction to the appropriate Kafka topic based on its stuck
     * stage.
     */
    private void sendToKafkaTopic(PendingTransaction pending,
            TransferMoneyRequest request,
            String stage) {
        switch (stage) {
            case "INITIATED":
            case "FRAUD_EVALUATE":
                // Both stages should restart from beginning
                log.info("Retrying transaction {} from start-transfer-money topic", pending.getId());
                transferMoneyKafkaTemplate.send("start-transfer-money", request);
                break;

            default:
                log.warn("Unknown stage {} for transaction {}, sending to start-transfer-money",
                        stage, pending.getId());
                transferMoneyKafkaTemplate.send("start-transfer-money", request);
        }

        log.info("Successfully re-queued stuck transaction {} for retry", pending.getId());
    }

    /**
     * Cleanup old completed/cancelled transactions.
     * Runs daily at 2 AM.
     * Uses soft delete by marking transactions as archived.
     */
    @Scheduled(cron = "0 0 2 * * *") // Every day at 2 AM
    @Transactional
    public void cleanupOldTransaction() {
        log.info("Running old transaction cleanup...");

        // Find transactions older than 30 days that are completed, cancelled, or failed
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        List<TransactionStatus> archivableStatuses = List.of(
                TransactionStatus.COMPLETED,
                TransactionStatus.CANCELLED,
                TransactionStatus.FAILED);

        List<PendingTransaction> oldTransactions = pendingTransactionRepository
                .findOldTransactionsForArchival(archivableStatuses, threshold);

        if (oldTransactions.isEmpty()) {
            log.info("No transactions found for archival");
            return;
        }

        int archivedCount = 0;
        for (PendingTransaction transaction : oldTransactions) {
            try {
                archiveTransaction(transaction);
                archivedCount++;
            } catch (Exception e) {
                log.error("Failed to archive transaction {}: {}",
                        transaction.getId(), e.getMessage());
            }
        }

        log.info("Archived {} transactions older than 30 days", archivedCount);
    }

    /**
     * Marks a transaction as archived (soft delete).
     */
    private void archiveTransaction(PendingTransaction transaction) {
        transaction.setArchived(true);
        transaction.setArchivedAt(LocalDateTime.now());
        pendingTransactionRepository.save(transaction);

        log.debug("Archived transaction: id={}, status={}, createdAt={}",
                transaction.getId(),
                transaction.getStatus(),
                transaction.getCreatedAt());
    }

    private void sendNotification(String userId, String title, String message, String type) {
        if (userId == null) {
            log.warn("Cannot send notification: userId is null");
            return;
        }

        try {
            notificationKafkaTemplate.send("notification-service", SendNotificationRequest.builder()
                    .userId(userId)
                    .title(title)
                    .message(message)
                    .type(type)
                    .build());
        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage());
        }
    }
}
