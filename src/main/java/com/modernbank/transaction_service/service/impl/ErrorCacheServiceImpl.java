package com.modernbank.transaction_service.service.impl;

import com.modernbank.transaction_service.api.client.ParameterServiceClient;
import com.modernbank.transaction_service.api.response.GetAllErrorCodesResponse;
import com.modernbank.transaction_service.api.response.GetErrorCodeResponse;
import com.modernbank.transaction_service.entity.ErrorCodes;
import com.modernbank.transaction_service.service.ErrorCacheService;
import com.modernbank.transaction_service.service.MapperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j

public class ErrorCacheServiceImpl implements ErrorCacheService {

    private final MapperService mapperService;

    private final RedisTemplate<String, ErrorCodes> redisTemplate;

    private static final String ERROR_CODES_CACHE_KEY = "ERROR_CODE_CACHE";

    private final ParameterServiceClient parameterServiceClient;

    @Override
    public ErrorCodes getErrorCodeByErrorId(String code){
        String cacheKey = ERROR_CODES_CACHE_KEY + code;
        ErrorCodes errorCode = redisTemplate.opsForValue().get(cacheKey);

        if(errorCode != null){
            return errorCode;
        }
        GetErrorCodeResponse response;
        try{
            response = parameterServiceClient.getErrorCode(code);
        }catch (Exception e){
            return handleErrorGetFailed(code);
        }
        redisTemplate.opsForValue().set(cacheKey, mapperService.map(response.getErrorCode(), ErrorCodes.class));

        return mapperService.map(response.getErrorCode(), ErrorCodes.class);
    }

    @Scheduled(fixedRate = 3600000) // every hour
    @Override
    public void refreshAllErrorCodesCache() {
        try {
            GetAllErrorCodesResponse allErrorCodes = parameterServiceClient.getAllErrorCodes("transaction-service");
            log.info("Refreshing error codes cache");

            allErrorCodes.getErrorCodes().forEach(errorCodeDTO-> {
                String code = errorCodeDTO.getError();
                String cacheKey = ERROR_CODES_CACHE_KEY + code;
                ErrorCodes errorCode = mapperService.map(errorCodeDTO, ErrorCodes.class);
                redisTemplate.opsForValue().set(cacheKey, errorCode);
            });
        } catch (Exception e) {
            log.error("Error refreshing error codes cache, keeping existing cache.");
        }
    }

    private ErrorCodes handleErrorGetFailed(String code){
        String traceId = MDC.get("traceId");
        return ErrorCodes.builder()
                .id(code != null ? code : "UNKNOWN")
                .error("Sistem Hatası")
                .description("Beklenmeyen bir hata oluştu. Lütfen destek ekibiyle iletişime geçin. Takip Numarasi: " + traceId)
                .httpStatus(500)
                .build();
    }
}