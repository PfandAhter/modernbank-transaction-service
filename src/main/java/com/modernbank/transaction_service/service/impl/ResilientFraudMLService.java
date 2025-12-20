package com.modernbank.transaction_service.service.impl;

import com.modernbank.transaction_service.api.client.FraudMLServiceClient;
import com.modernbank.transaction_service.api.request.FraudCheckRequest;
import com.modernbank.transaction_service.api.response.FraudCheckResponse;
import com.modernbank.transaction_service.entity.FraudEvaluation;
import com.modernbank.transaction_service.model.enums.FraudDecisionAction;
import com.modernbank.transaction_service.model.enums.RiskLevel;
import com.modernbank.transaction_service.repository.FraudEvaluationRepository;
import com.modernbank.transaction_service.service.util.FeatureVectorSerializer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Resilient wrapper for Fraud ML Service calls.
 * <p>
 * Features:
 * - Circuit Breaker: Prevents cascading failures
 * - Metrics: Tracks success/failure rates, latency
 * - Fail-Open: Returns LOW risk when service is unavailable
 * <p>
 * CRITICAL: When ML service is down, transactions continue (fail-open).
 * Never block transactions due to ML service issues.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResilientFraudMLService {

    private final FraudMLServiceClient fraudMLClient;
    private final MeterRegistry meterRegistry;

    // Metrics
    private Counter fraudServiceSuccessCounter;
    private Counter fraudServiceFailureCounter;
    private Counter fraudServiceFallbackCounter;
    private Timer fraudServiceLatencyTimer;

    @PostConstruct
    public void initMetrics() {
        fraudServiceSuccessCounter = Counter.builder("fraud.ml.service.calls")
                .tag("result", "success")
                .description("Successful calls to Fraud ML Service")
                .register(meterRegistry);

        fraudServiceFailureCounter = Counter.builder("fraud.ml.service.calls")
                .tag("result", "failure")
                .description("Failed calls to Fraud ML Service")
                .register(meterRegistry);

        fraudServiceFallbackCounter = Counter.builder("fraud.ml.service.calls")
                .tag("result", "fallback")
                .description("Fallback invocations when circuit is open or service fails")
                .register(meterRegistry);

        fraudServiceLatencyTimer = Timer.builder("fraud.ml.service.latency")
                .description("Latency of Fraud ML Service calls")
                .register(meterRegistry);
    }

    /**
     * Evaluate transaction for fraud risk with circuit breaker protection.
     * <p>
     * Circuit Breaker States:
     * - CLOSED: Normal operation, calls go through
     * - OPEN: Failures exceeded threshold, directly calls fallback (no network
     * call)
     * - HALF_OPEN: Trying to recover, limited calls go through
     *
     * @param request Fraud check request with transaction details
     * @return Risk score and level (fallback returns LOW risk)
     */
    @CircuitBreaker(name = "fraudMLService", fallbackMethod = "evaluateTransactionFallback")
    public FraudCheckResponse evaluateTransaction(FraudCheckRequest request) {
        Instant start = Instant.now();
        try {
            log.debug("Calling Fraud ML Service for transaction: {}", request.getPendingTransactionId());

            FraudCheckResponse response = fraudMLClient.evaluateTransaction(request);

            // Record success metrics
            fraudServiceSuccessCounter.increment();
            fraudServiceLatencyTimer.record(Duration.between(start, Instant.now()));

            log.info("Fraud ML response: transactionId={}, riskScore={}, riskLevel={}",
                    request.getPendingTransactionId(),
                    response.getRiskScore(),
                    response.getRiskLevel());

            return response;

        } catch (Exception e) {
            // Record failure metrics (circuit breaker will handle the exception)
            fraudServiceFailureCounter.increment();
            fraudServiceLatencyTimer.record(Duration.between(start, Instant.now()));
            log.error("Fraud ML Service call failed: {}", e.getMessage());
            throw e; // Re-throw so circuit breaker can track it
        }
    }

    /**
     * Fallback method when circuit is OPEN or call fails.
     * <p>
     * CRITICAL DESIGN DECISION: FAIL-OPEN
     * - Returns LOW risk so transactions continue
     * - Logs warning for monitoring/alerting
     * - Records metrics for dashboards
     * <p>
     * This ensures ML service outages don't block the entire banking system.
     */
    @SuppressWarnings("unused") // Used by @CircuitBreaker annotation
    private FraudCheckResponse evaluateTransactionFallback(FraudCheckRequest request, Throwable t) {
        // Record fallback metrics
        fraudServiceFallbackCounter.increment();

        log.warn("FRAUD_ML_FALLBACK: Using default LOW risk for transaction {}. Reason: {} - {}",
                request.getPendingTransactionId(),
                t.getClass().getSimpleName(),
                t.getMessage());

        // Return LOW risk - let the transaction proceed
        return FraudCheckResponse.builder()
                .riskScore(0.1)
                .riskLevel("LOW")
                .recommendedAction("APPROVE")
                .modelVersion("FALLBACK")
                .build();
    }

    /**
     * Check if circuit breaker is currently open (for health checks).
     */
    public boolean isCircuitOpen() {
        // This would require injecting CircuitBreakerRegistry if needed
        return false; // Placeholder - actuator endpoints provide this info
    }
}
