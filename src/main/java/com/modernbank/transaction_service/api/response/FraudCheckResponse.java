package com.modernbank.transaction_service.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FraudCheckResponse {

    /**
     * Risk score from 0.0 to 1.0
     * < 0.30 = LOW
     * 0.30 - 0.70 = MEDIUM
     * > 0.70 = HIGH
     */
    private Double riskScore;

    private String riskLevel;

    private String recommendedAction;

    private LocalDateTime evaluatedAt;

    /**
     * Confidence score of the ML prediction (0.0 - 1.0)
     */
//    private Double confidence; // TODO: May be added in future..

    private Map<String, Double> featureImportance;

    private String modelVersion;
}