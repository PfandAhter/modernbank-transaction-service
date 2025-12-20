package com.modernbank.transaction_service.repository;

import com.modernbank.transaction_service.entity.PendingTransaction;
import com.modernbank.transaction_service.model.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PendingTransactionRepository extends JpaRepository<PendingTransaction, String> {

        /**
         * Find pending transactions by status.
         */
        List<PendingTransaction> findByStatus(TransactionStatus status);

        /**
         * Find pending transactions by sender IBAN.
         */
        List<PendingTransaction> findBySenderIban(String senderIban);

        /**
         * Find HOLD transactions that have expired (for auto-cancel).
         */
        @Query("SELECT pt FROM PendingTransaction pt WHERE pt.status = :status " +
                        "AND pt.holdExpiresAt < :now")
        List<PendingTransaction> findByStatusAndHoldExpiresAtBefore(
                        @Param("status") TransactionStatus status,
                        @Param("now") LocalDateTime now);

        /**
         * Find transactions in a specific stage that are stuck (for recovery).
         */
        @Query("SELECT pt FROM PendingTransaction pt WHERE pt.currentStage = :stage " +
                        "AND pt.updatedAt < :threshold")
        List<PendingTransaction> findStuckTransactions(
                        @Param("stage") String stage,
                        @Param("threshold") LocalDateTime threshold);

        /**
         * Find by sender account ID and status.
         */
        List<PendingTransaction> findBySenderAccountIdAndStatus(
                        String senderAccountId, TransactionStatus status);

        /**
         * Find transactions requiring strong auth that haven't been verified.
         */
        @Query("SELECT pt FROM PendingTransaction pt WHERE pt.requiresStrongAuth = true " +
                        "AND pt.status = :status " +
                        "AND pt.authCodeExpiresAt < :now")
        List<PendingTransaction> findExpiredStrongAuthTransactions(
                        @Param("status") TransactionStatus status,
                        @Param("now") LocalDateTime now);

        /**
         * Find old completed/cancelled transactions for archival.
         * Excludes already archived transactions.
         */
        @Query("SELECT pt FROM PendingTransaction pt " +
                        "WHERE pt.status IN :statuses " +
                        "AND pt.createdAt < :threshold " +
                        "AND (pt.archived IS NULL OR pt.archived = false)")
        List<PendingTransaction> findOldTransactionsForArchival(
                        @Param("statuses") List<TransactionStatus> statuses,
                        @Param("threshold") LocalDateTime threshold);
}
