package com.modernbank.transaction_service.entity;

import com.modernbank.transaction_service.model.enums.FraudDecisionAction;
import com.modernbank.transaction_service.model.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity to store ML fraud signals and decision outcomes for each transaction.
 * These signals are mandatory for future credit scoring models.
 */
@Entity
@Table(name = "fraud_decisions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FraudDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "pending_transaction_id")
    private String pendingTransactionId;

    @Column(name = "account_id")
    private String accountId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "sender_iban")
    private String senderIban;

    @Column(name = "receiver_iban")
    private String receiverIban;

    @Column(name = "amount")
    private Double amount;

    // ML signals - mandatory for credit scoring
    @Column(name = "risk_score")
    private Double riskScore;

    @Column(name = "risk_level")
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @Column(name = "amount_to_balance_ratio")
    private Double amountToBalanceRatio;

    @Column(name = "new_receiver_flag")
    private Boolean newReceiverFlag;

    // Decision outcome
    @Column(name = "decision_taken")
    @Enumerated(EnumType.STRING)
    private FraudDecisionAction decisionTaken;

    @Column(name = "confirmation_result")
    private String confirmationResult; // USER_CONFIRMED, TIMEOUT, BLOCKED, OTP_VERIFIED

    @Column(name = "time_to_confirm")
    private Long timeToConfirm; // milliseconds

    // Timestamps
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    // Additional context for audit
    @Column(name = "ml_recommended_action")
    private String mlRecommendedAction;

    @Column(name = "decision_reason")
    private String decisionReason;

    @Column(name = "high_risk_count_24h")
    private Integer highRiskCount24h;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
