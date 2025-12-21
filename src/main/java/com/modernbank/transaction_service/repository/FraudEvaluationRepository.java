package com.modernbank.transaction_service.repository;

import com.modernbank.transaction_service.entity.FraudEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FraudEvaluationRepository extends JpaRepository<FraudEvaluation, String> {

}