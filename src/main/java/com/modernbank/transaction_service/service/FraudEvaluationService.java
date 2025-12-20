package com.modernbank.transaction_service.service;

import com.modernbank.transaction_service.entity.Transaction;
import com.modernbank.transaction_service.model.enums.FraudDecision;

/**
 * Synchronous Fraud Evaluation Service.
 * 
 * Called BEFORE starting any Kafka-based money transfer flow.
 * 
 * Responsibilities:
 * - Call Fraud Detection Service synchronously
 * - Persist fraud evaluation results in transaction record
 * - Return decision (APPROVE/HOLD/BLOCK)
 * 
 * This service does NOT:
 * - Block accounts directly
 * - Decide block duration
 * - Persist block state
 */
public interface FraudEvaluationService {

    /**
     * Evaluate transaction for fraud risk BEFORE starting Kafka flow.
     * 
     * Flow:
     * 1. Build fraud check request with transaction details
     * 2. Call Fraud Detection Service (synchronous, with circuit breaker)
     * 3. Map riskLevel to decision: LOW→APPROVE, MEDIUM→HOLD, HIGH→BLOCK
     * 4. Update transaction entity with fraud results
     * 5. Return decision for caller to act on
     * 
     * @param transaction     Transaction to evaluate (will be updated with fraud
     *                        fields)
     * @param senderAccountId Sender's account ID for profile lookup
     * @return FraudDecision - APPROVE, HOLD, or BLOCK
     */
    FraudDecision evaluateAndDecide(Transaction transaction, String senderAccountId);

    /**
     * Check if account is temporarily blocked before allowing transfer.
     * 
     * @param accountId Account to check
     * @return true if account is blocked
     */
    boolean isAccountBlocked(String accountId);

    /**
     * Increment fraud counter in Account Service.
     * Called when HIGH risk (BLOCK) decision is made.
     * 
     * @param userId User whose fraud count should be incremented
     * @param reason Reason for increment
     */
    void incrementFraudCounter(String userId, String reason);
}
