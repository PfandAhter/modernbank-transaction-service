package com.modernbank.transaction_service.entity;

import com.modernbank.transaction_service.model.enums.FraudDecisionAction;
import com.modernbank.transaction_service.model.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_evaluation")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "risk_score")
    private Double riskScore;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    private FraudDecisionAction recommendedAction;

    @Column(columnDefinition = "json")
    private String featureVector; // Map<String, Double> -> JSON

    @Column(columnDefinition = "json")
    private String featureImportance; // Map<String, Double> -> JSON

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}