package com.modernbank.transaction_service.model;

import com.modernbank.transaction_service.entity.FraudEvaluation;
import com.modernbank.transaction_service.entity.Transaction;
import io.github.resilience4j.core.lang.Nullable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnrichedTransaction {
    private Transaction transaction;

    @Nullable
    private FraudEvaluation fraudEvaluation;
}