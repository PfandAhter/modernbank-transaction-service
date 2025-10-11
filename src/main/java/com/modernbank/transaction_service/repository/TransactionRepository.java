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
}