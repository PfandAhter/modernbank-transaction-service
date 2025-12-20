package com.modernbank.transaction_service.model.enums;

/**
 * Risk level classification based on ML fraud score.
 * LOW: score < 0.30
 * MEDIUM: 0.30 <= score <= 0.70
 * HIGH: score > 0.70
 */
public enum RiskLevel {
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH");

    private final String level;

    RiskLevel(String level) {
        this.level = level;
    }

    public String getLevel() {
        return level;
    }

    public static RiskLevel fromScore(double score) {
        if (score < 0.30)
            return LOW;
        if (score <= 0.70)
            return MEDIUM;
        return HIGH;
    }
}
