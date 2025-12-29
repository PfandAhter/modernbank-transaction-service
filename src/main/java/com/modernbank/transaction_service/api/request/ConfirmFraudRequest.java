package com.modernbank.transaction_service.api.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for confirming fraud.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmFraudRequest {

    private String pendingTransactionId;

    /**
     * Who is confirming: ANALYST, USER_REPORT, CHARGEBACK
     */
    private String confirmedBy;

    /**
     * Reason for confirmation
     */
    private String reason;

    /**
     * Whether to block the user's account
     */
    private boolean blockAccount;
}
