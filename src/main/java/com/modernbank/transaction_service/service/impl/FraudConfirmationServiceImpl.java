package com.modernbank.transaction_service.service.impl;

import com.modernbank.transaction_service.api.client.AccountServiceClient;
import com.modernbank.transaction_service.api.request.TransactionAdditionalApproveRequest;
import com.modernbank.transaction_service.api.request.TransferMoneyRequest;
import com.modernbank.transaction_service.api.response.GetAccountByIban;
import com.modernbank.transaction_service.api.response.GetAccountByIdResponse;
import com.modernbank.transaction_service.entity.FraudDecision;
import com.modernbank.transaction_service.entity.PendingTransaction;
import com.modernbank.transaction_service.entity.Transaction;
import com.modernbank.transaction_service.exception.NotFoundException;
import com.modernbank.transaction_service.model.enums.TransactionStatus;
import com.modernbank.transaction_service.repository.FraudDecisionRepository;
import com.modernbank.transaction_service.repository.PendingTransactionRepository;
import com.modernbank.transaction_service.repository.TransactionRepository;
import com.modernbank.transaction_service.service.FraudConfirmationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for handling fraud confirmation.
 * 
 * Called when:
 * - Fraud analyst confirms a transaction was fraudulent
 * - User reports an unauthorized transaction
 * - Chargeback is received
 * 
 * This increments previousFraudCount in Account Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FraudConfirmationServiceImpl implements FraudConfirmationService {

    private final FraudDecisionRepository fraudDecisionRepository;
    private final PendingTransactionRepository pendingTransactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, TransferMoneyRequest> transferMoneyKafkaTemplate;

    /**
     * Confirm a transaction as fraudulent.
     * This will:
     * 1. Update FraudDecision with CONFIRMED_FRAUD
     * 2. Cancel the transaction if still pending
     * 3. Increment user's previousFraudCount in Account Service
     * 4. Optionally block the account
     * 
     * @param pendingTransactionId ID of the pending transaction
     * @param confirmedBy          Who confirmed (e.g., "ANALYST", "USER_REPORT",
     *                             "CHARGEBACK")
     * @param reason               Reason for confirmation
     * @param blockAccount         Whether to block the account
     */
    @Transactional
    @Override
    public void confirmFraud(
            String pendingTransactionId,
            String confirmedBy,
            String reason,
            boolean blockAccount) {
        log.warn("Confirming fraud for pendingTransactionId: {}, by: {}",
                pendingTransactionId, confirmedBy);

        // 1. Update FraudDecision
        FraudDecision decision = fraudDecisionRepository.findByPendingTransactionId(pendingTransactionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Fraud decision not found for: " + pendingTransactionId));

        decision.setConfirmationResult("CONFIRMED_FRAUD");
        decision.setDecidedAt(LocalDateTime.now());
        decision.setDecisionReason(reason + " (Confirmed by: " + confirmedBy + ")");
        fraudDecisionRepository.save(decision);

        // 2. Cancel the transaction if still pending/hold
        PendingTransaction pending = pendingTransactionRepository.findById(pendingTransactionId)
                .orElse(null);

        if (pending != null &&
                (pending.getStatus() == TransactionStatus.HOLD ||
                        pending.getStatus() == TransactionStatus.INITIATED ||
                        pending.getStatus() == TransactionStatus.FRAUD_REVIEW)) {

            pending.setStatus(TransactionStatus.BLOCKED);
            pending.setCurrentStage("FRAUD_CONFIRMED");
            pending.setLastError("Fraud confirmed by " + confirmedBy);
            pendingTransactionRepository.save(pending);
            log.info("Transaction {} blocked due to confirmed fraud", pendingTransactionId);
        }

        // 3. Increment previousFraudCount in Account Service
        try {
            accountServiceClient.confirmFraud(decision.getAccountId(), reason);
            log.info("Incremented fraud count for accountId: {}", decision.getAccountId());
        } catch (Exception e) {
            log.error("Failed to increment fraud count for accountId {}: {}",
                    decision.getAccountId(), e.getMessage());
            // Don't fail the whole operation - fraud is still confirmed locally
        }

        // 4. Block account if requested
        if (blockAccount) {
            try {
                accountServiceClient.holdAccount(decision.getAccountId());
                log.info("Account {} blocked due to confirmed fraud", decision.getAccountId());
            } catch (Exception e) {
                log.error("Failed to block account {}: {}", decision.getAccountId(), e.getMessage());
            }
        }

        log.warn("Fraud confirmed: pendingTransactionId={}, userId={}, reason={}",
                pendingTransactionId, decision.getUserId(), reason);
    }

    /**
     * Mark a HOLD transaction as legitimate (false positive).
     * User confirmed they made the transaction.
     */
    @Transactional
    @Override
    public void confirmLegitimate(String pendingTransactionId, String confirmedBy) {
        log.info("Confirming legitimate transaction: {}", pendingTransactionId);

        FraudDecision decision = fraudDecisionRepository.findByPendingTransactionId(pendingTransactionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Fraud decision not found for: " + pendingTransactionId));

        decision.setConfirmationResult("FALSE_POSITIVE");
        decision.setDecidedAt(LocalDateTime.now());
        decision.setTimeToConfirm(
                java.time.Duration.between(decision.getCreatedAt(), LocalDateTime.now()).toMillis());
        fraudDecisionRepository.save(decision);

        // This data helps train better ML models - false positives are valuable
        // feedback
        log.info("Transaction {} confirmed as legitimate (false positive)", pendingTransactionId);
    }

    @Override
    public void confirmAdditionalTransaction(TransactionAdditionalApproveRequest additionalApproveRequest) {
        Transaction transaction = transactionRepository.findById(additionalApproveRequest.getTransactionId())
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        if (!transaction.getStatus().equals(TransactionStatus.HOLD)) {
            throw new IllegalStateException("Transaction is not in HOLD status");
        }

        // İşlemi APPROVED durumuna alın
        transaction.setStatus(TransactionStatus.APPROVED);
        transactionRepository.save(transaction);

        // Transfer request'i yeniden oluşturun
        TransferMoneyRequest request = reconstructTransferRequest(transaction);
        GetAccountByIdResponse account = accountServiceClient.getAccountById(transaction.getAccountId());

        request.setFromIBAN(account.getAccount().getIban());
        // Bakiyeyi düşün
        accountServiceClient.updateBalance(account.getAccount().getIban(), -transaction.getAmount());

        transaction.setStatus(TransactionStatus.PENDING);
        transactionRepository.save(transaction);

        transferMoneyKafkaTemplate.send("update-transfer-money", request);
        log.info("HOLD transaction confirmed and resumed: transactionId={}", additionalApproveRequest.getTransactionId());
    }

    private TransferMoneyRequest reconstructTransferRequest(Transaction transaction) {
        return TransferMoneyRequest.builder()
                .senderTransactionId(transaction.getId())
                .toIBAN(transaction.getReceiverIban())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .toFirstName(transaction.getReceiverFirstName())
                .toSecondName(transaction.getReceiverSecondName())
                .toLastName(transaction.getReceiverLastName())
                .build();
    }
}
