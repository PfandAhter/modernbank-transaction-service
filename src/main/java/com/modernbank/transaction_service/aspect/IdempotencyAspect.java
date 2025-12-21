package com.modernbank.transaction_service.aspect;

import com.modernbank.transaction_service.api.response.BaseResponse;
import com.modernbank.transaction_service.constant.HeaderKey;
import com.modernbank.transaction_service.exception.DuplicateRequestException;
import com.modernbank.transaction_service.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static com.modernbank.transaction_service.constant.HeaderKey.USER_ID;
import static com.modernbank.transaction_service.constant.HeaderKey.IDEMPOTENCY_KEY;
import static com.modernbank.transaction_service.constant.ErrorCodeConstants.DUPLICATE_REQUEST;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyAspect {


    private final IdempotencyService idempotencyService;

    private static final String IDEMPOTENCY_KEY_HEADER = IDEMPOTENCY_KEY;
    private static final String USER_ID_HEADER = USER_ID;

    @Around("@annotation(com.modernbank.transaction_service.validator.annotation.Idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getCurrentRequest();

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        String userId = request.getHeader(USER_ID_HEADER);

        // If no idempotency key provided, proceed normally
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            log.warn("No idempotency key provided, proceeding without check");
            return joinPoint.proceed();
        }

        // Check for cached response
        String cachedResponse = idempotencyService.getCachedResponse(idempotencyKey, userId);
        if (cachedResponse != null && !cachedResponse.equals("PROCESSING")) {
            log.info("Returning cached response for key: {}", idempotencyKey);
            return ResponseEntity.ok(new BaseResponse("CACHED", cachedResponse));
        }

        // Try to acquire lock
        if (!idempotencyService.tryAcquire(idempotencyKey, userId)) {
            throw new DuplicateRequestException(DUPLICATE_REQUEST);
        }

        try {
            // Execute the actual method
            Object result = joinPoint.proceed();

            // Cache the response
            String responseToCache = extractResponseMessage(result);
            idempotencyService.markCompleted(idempotencyKey, userId, responseToCache);

            return result;
        } catch (Exception e) {
            // On failure, release the key to allow retry
            idempotencyService.release(idempotencyKey, userId);
            throw e;
        }
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes.getRequest();
    }

    private String extractResponseMessage(Object result) {
        if (result instanceof ResponseEntity<?> responseEntity) {
            Object body = responseEntity.getBody();
            if (body instanceof BaseResponse baseResponse) {
                return baseResponse.getProcessMessage();
            }
        }
        return "SUCCESS";
    }
}
