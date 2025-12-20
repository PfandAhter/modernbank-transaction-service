package com.modernbank.transaction_service.api.client;

import com.modernbank.transaction_service.api.request.FraudCheckRequest;
import com.modernbank.transaction_service.api.response.FraudCheckResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for Fraud ML Service.
 * The ML service provides risk_score, risk_level, and recommended_action.
 * IMPORTANT: ML does NOT make final decisions - business rules decide.
 */
@FeignClient(value = "fraud-ml-service", url = "${feign.client.fraud-service.url}")
public interface FraudMLServiceClient {

    /**
     * Evaluate a transaction for fraud risk.
     * 
     * @param request Transaction and account profile data
     * @return Risk score, level, and ML recommendation
     */
    @PostMapping("${feign.client.fraud-service.evaluate:/api/v1/fraud/evaluate}")
    FraudCheckResponse evaluateTransaction(@RequestBody FraudCheckRequest request);
}
