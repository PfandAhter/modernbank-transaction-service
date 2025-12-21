package com.modernbank.transaction_service.service.impl;

import com.modernbank.transaction_service.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyServiceImpl implements IdempotencyService {
    private final RedisTemplate<String, String> redisTemplate;

    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";

    @Value("${idempotency.key-ttl-minutes:30}")
    private long keyTtlMinutes;

    /**
     * Check if request is duplicate. If not, mark it as processing.
     * @return true if this is a NEW request, false if DUPLICATE
     */
    @Override
    public boolean tryAcquire(String idempotencyKey, String userId) {
        String redisKey = buildKey(idempotencyKey, userId);

        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PROCESSING", keyTtlMinutes, TimeUnit.MINUTES);

        if (Boolean.TRUE.equals(isNew)) {
            log.info("New idempotency key acquired: {}", redisKey);
            return true;
        }

        log.warn("Duplicate request detected for key: {}", redisKey);
        return false;
    }

    /**
     * Mark request as completed with response
     */
    @Override
    public void markCompleted(String idempotencyKey, String userId, String response) {
        String redisKey = buildKey(idempotencyKey, userId);
        redisTemplate.opsForValue().set(redisKey, response, keyTtlMinutes, TimeUnit.MINUTES);
        log.info("Idempotency key marked completed: {}", redisKey);
    }

    /**
     * Get cached response if exists
     */
    @Override
    public String getCachedResponse(String idempotencyKey, String userId) {
        String redisKey = buildKey(idempotencyKey, userId);
        return redisTemplate.opsForValue().get(redisKey);
    }

    /**
     * Remove key (on failure, allow retry)
     */
    @Override
    public void release(String idempotencyKey, String userId) {
        String redisKey = buildKey(idempotencyKey, userId);
        redisTemplate.delete(redisKey);
        log.info("Idempotency key released: {}", redisKey);
    }

    private String buildKey(String idempotencyKey, String userId) {
        return IDEMPOTENCY_KEY_PREFIX + userId + ":" + idempotencyKey;
    }
}
