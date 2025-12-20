package com.modernbank.transaction_service.entity;

import com.modernbank.transaction_service.model.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity to track in-flight transfers for recoverability.
 * Used for timeout handling and compensation in the Saga pattern.
 */
@Entity
@Table(name = "pending_transactions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PendingTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "sender_iban", nullable = false)
    private String senderIban;

    @Column(name = "receiver_iban", nullable = false)
    private String receiverIban;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "description")
    private String description;

    @Column(name = "sender_user_id")
    private String senderUserId;

    @Column(name = "sender_account_id")
    private String senderAccountId;

    @Column(name = "receiver_first_name")
    private String receiverFirstName;

    @Column(name = "receiver_second_name")
    private String receiverSecondName;

    @Column(name = "receiver_last_name")
    private String receiverLastName;

    @Column(name = "by_ai")
    private String byAi;

    // Saga tracking
    @Column(name = "current_stage")
    private String currentStage; // INITIATED, FRAUD_EVALUATE, FRAUD_DECISION, UPDATE, FINALIZE

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    // Balance tracking for compensation
    @Column(name = "balance_debited")
    private Boolean balanceDebited;

    @Column(name = "debited_amount")
    private Double debitedAmount;

    // Timeout handling
    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Column(name = "requires_strong_auth")
    private Boolean requiresStrongAuth;

    @Column(name = "strong_auth_type")
    private String strongAuthType; // OTP, BIOMETRIC, IN_APP

    @Column(name = "auth_code")
    private String authCode; // For OTP verification

    @Column(name = "auth_code_expires_at")
    private LocalDateTime authCodeExpiresAt;

    // Timestamps
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Recovery tracking
    @Column(name = "retries_count")
    private Integer retriesCount;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "original_request_json", columnDefinition = "TEXT")
    private String originalRequestJson; // Serialized TransferMoneyRequest for replay

    // Archival tracking
    @Column(name = "archived")
    private Boolean archived;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        retriesCount = 0;
        balanceDebited = false;
        archived = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
