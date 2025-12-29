package com.modernbank.transaction_service.repository;

import com.modernbank.transaction_service.entity.FraudDecision;
import com.modernbank.transaction_service.model.enums.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FraudDecisionRepository extends JpaRepository<FraudDecision, String> {

    Optional<FraudDecision> findByPendingTransactionId(String pendingTransactionId);

    Optional<FraudDecision> findByTransactionId(String transactionId);

    List<FraudDecision> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Count HIGH risk transactions for a user in the last 24 hours.
     * Used to determine if we should BLOCK (>=2 HIGH in 24h rule).
     */
    @Query("SELECT COUNT(fd) FROM FraudDecision fd WHERE fd.userId = :userId " +
            "AND fd.riskLevel = :riskLevel " +
            "AND fd.createdAt >= :since")
    int countByUserIdAndRiskLevelSince(
            @Param("userId") String userId,
            @Param("riskLevel") RiskLevel riskLevel,
            @Param("since") LocalDateTime since);

    /**
     * Count confirmed fraud cases for a user.
     */
    @Query("SELECT COUNT(fd) FROM FraudDecision fd WHERE fd.userId = :userId " +
            "AND fd.confirmationResult = 'CONFIRMED_FRAUD'")
    int countConfirmedFraudByUserId(@Param("userId") String userId);

    /**
     * Find pending decisions that have expired (for timeout handling).
     */
    @Query("SELECT fd FROM FraudDecision fd WHERE fd.confirmationResult IS NULL " +
            "AND fd.expiresAt < :now")
    List<FraudDecision> findExpiredDecisions(@Param("now") LocalDateTime now);
}
