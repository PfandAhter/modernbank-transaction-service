package com.modernbank.transaction_service.api.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal message for fraud evaluation Kafka topic.
 * Contains all data needed for ML evaluation and decision making.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FraudEvaluationMessage {

    private String pendingTransactionId;

    // Original transfer request data
    private String fromIBAN;
    private String toIBAN;
    private Double amount;
    private String description;
    private String toFirstName;
    private String toSecondName;
    private String toLastName;
    private String byAi;

    // Sender info
    private String senderUserId;
    private String senderAccountId;

    // Channel info
    private String channel;
    private String deviceFingerprint;
    private String ipAddress;
}
