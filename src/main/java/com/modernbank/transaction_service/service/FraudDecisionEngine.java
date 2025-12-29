package com.modernbank.transaction_service.service;

import com.modernbank.transaction_service.api.response.AccountProfileResponse;
import com.modernbank.transaction_service.api.response.FraudCheckResponse;
import com.modernbank.transaction_service.entity.FraudDecision;

/**
 * Fraud Decision Engine Interface.
 * 
 * Core Principles (DO NOT VIOLATE):
 * - AI suggests risk – business rules decide
 * - Money must not move while risk is unresolved
 * - First-time high risk = HOLD, not BLOCK
 * - Repeated or confirmed risk = BLOCK
 * - Account data is read-only from Account Service
 */
public interface FraudDecisionEngine {

    /**
     * Evaluate and decide on a transaction based on ML response and account
     * profile.
     * 
     * Decision Matrix:
     * - LOW risk (< 0.30) → APPROVE
     * - MEDIUM risk (0.30 - 0.70) → HOLD + Notify
     * - HIGH risk (first occurrence) → HOLD_STRONG_AUTH
     * - HIGH risk (≥2 in 24h) → BLOCK
     * - HIGH + previous confirmed fraud → BLOCK
     * - Known blacklisted receiver → BLOCK
     *
     * @param mlResponse           Response from fraud ML service
     * @param accountProfile       Account profile from Account Service (read-only)
     * @param receiverIban         Receiver IBAN to check blacklist
     * @param pendingTransactionId ID of the pending transaction
     * @return FraudDecision entity with decision and signals
     */
    FraudDecision evaluate(
            FraudCheckResponse mlResponse,
            AccountProfileResponse accountProfile,
            String receiverIban,
            String pendingTransactionId,
            Double amount);

    /**
     * Confirm a HOLD transaction after user verification.
     * 
     * @param pendingTransactionId ID of the pending transaction
     * @param confirmationType     Type of confirmation (USER_CONFIRMED,
     *                             OTP_VERIFIED, BIOMETRIC)
     * @return true if confirmation successful
     */
    boolean confirmHoldTransaction(String pendingTransactionId, String confirmationType);

    /**
     * Timeout a HOLD transaction that wasn't confirmed in time.
     * 
     * @param pendingTransactionId ID of the pending transaction
     */
    void timeoutHoldTransaction(String pendingTransactionId);
}
