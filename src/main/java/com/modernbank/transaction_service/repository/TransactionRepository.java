package com.modernbank.transaction_service.repository;

import com.modernbank.transaction_service.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    Page<Transaction> findByAccountId(String accountId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.accountId = ?1 AND t.id = ?2")
    Transaction findTransactionByAccountId(String accountId, String transactionId);
}