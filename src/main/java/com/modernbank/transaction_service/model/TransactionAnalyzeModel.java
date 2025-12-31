package com.modernbank.transaction_service.model;

import com.modernbank.transaction_service.model.enums.AnalyzeRange;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionAnalyzeModel {

    /**
     * The requested analysis range.
     */
    private AnalyzeRange analyzeRange;

    /**
     * Transactions from the current analysis period.
     */
    private List<EnrichedTransaction> currentPeriodTransactions;

    /**
     * Transactions from the previous period (for comparison/trend analysis).
     */
    private List<EnrichedTransaction> previousPeriodTransactions;

    /**
     * Total historical transaction count for dynamic threshold calculation.
     */
    private Integer totalHistoricalTransactionCount;

    /**
     * User's historical average transaction amount.
     */
    private Double historicalAverageAmount;
}