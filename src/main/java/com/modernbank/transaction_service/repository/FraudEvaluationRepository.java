package com.modernbank.transaction_service.repository;

import com.modernbank.transaction_service.entity.FraudEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface FraudEvaluationRepository extends JpaRepository<FraudEvaluation, String> {

    List<FraudEvaluation> findByTransactionIdIn(Collection<String> transactionIds);
}