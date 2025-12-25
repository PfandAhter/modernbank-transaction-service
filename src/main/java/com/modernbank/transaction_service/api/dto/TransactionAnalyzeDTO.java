package com.modernbank.transaction_service.api.dto;

import com.modernbank.transaction_service.model.enums.FraudDecisionAction;
import com.modernbank.transaction_service.model.enums.RiskLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter

public class TransactionAnalyzeDTO {
    private String transactionId;

    private Double riskScore;

    private RiskLevel riskLevel;

    private FraudDecisionAction fraudDecisionAction;

    private String featureVector;

    private String featureImportance;

    private String modelVersion;

    private LocalDateTime createdAt;
}