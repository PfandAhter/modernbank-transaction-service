package com.modernbank.transaction_service.service;

public interface IdempotencyService {
    boolean tryAcquire(String idempotencyKey, String userId);

    void markCompleted(String idempotencyKey, String userId, String response);

    String getCachedResponse(String idempotencyKey, String userId);

    void release(String idempotencyKey, String userId);
}