package com.modernbank.transaction_service.repository;

import com.modernbank.transaction_service.entity.Transaction;
import com.modernbank.transaction_service.model.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    Page<Transaction> findByAccountId(String accountId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.accountId = ?1 AND t.id = ?2")
    Transaction findTransactionByAccountId(String accountId, String transactionId);

    @Query("SELECT t FROM Transaction t WHERE t.accountId =:accountId " +
            "AND (:type IS NULL OR t.type = :type) " +
            "AND (:startDate IS NULL OR t.date >= :startDate)" +
            "AND (:endDate IS NULL OR t.date <= :endDate)" +
            "ORDER BY t.date DESC")
    Page<Transaction> findAllByAccountIdAndTypeAndDateBetween
            (@Param("accountId") String accountId,
             @Param("type") TransactionType type,
             @Param("startDate") LocalDateTime startDate,
             @Param("endDate") LocalDateTime endDate,
             Pageable pageable);

    @Query("SELECT COUNT(t) > 0 FROM Transaction t " +
            "WHERE t.accountId = :accountId " +
            "AND t.receiverIban = :receiverIban " +
            "AND t.amount = :amount " +
            "AND t.description = :description " +
            "AND t.date BETWEEN :start AND :end")
    boolean existsDuplicateTransaction(
            @Param("accountId") String accountId,
            @Param("receiverIban") String receiverIban,
            @Param("amount") double amount,
            @Param("description") String description,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT COUNT(t) > 0 FROM Transaction t WHERE t.accountId = :accountId " +
            "AND t.amount = :amount AND t.type = :type " +
            "AND t.date BETWEEN :startTime AND :endTime")
    boolean existsDuplicateWithdrawDeposit(
            @Param("accountId") String accountId,
            @Param("amount") Double amount,
            @Param("type") TransactionType type,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("""
    SELECT t FROM Transaction t
    WHERE t.accountId IN :accountIds
    AND t.date BETWEEN :start AND :end
""")
    Optional<List<Transaction>> findTransactionsForAnalysis(
            @Param("accountIds") List<String> accountIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );


    @Query("SELECT COALESCE(AVG(t.amount), 0) FROM Transaction t WHERE t.accountId = :senderAccountId AND t.date >= :since")
    Double findAvgAmountSinceBySender(@Param("senderAccountId") String senderAccountId, @Param("since") LocalDateTime since);

    Integer countByAccountIdAndDateAfter(String senderAccountId, LocalDateTime since);

    Integer countByAccountIdAndDateBetween(String senderAccountId, LocalDateTime start, LocalDateTime end);

    boolean existsByAccountIdAndReceiverIbanAndDateBefore(String senderAccountId, String receiverIban, LocalDateTime before);
}