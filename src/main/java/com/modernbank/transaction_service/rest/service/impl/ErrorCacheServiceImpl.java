package com.modernbank.transaction_service.rest.service.impl;

import com.modernbank.transaction_service.exception.ErrorCodesNotFoundException;
import com.modernbank.transaction_service.entity.ErrorCodes;
import com.modernbank.transaction_service.repository.ErrorCodesRepository;
import com.modernbank.transaction_service.rest.service.IErrorCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j

public class ErrorCacheServiceImpl implements IErrorCacheService {

    private final ErrorCodesRepository errorCodesRepository;

    @Cacheable(value = "errorCodes", key = "'all'")
    @Override
    public List<ErrorCodes> getAllErrorCodes(){
        return errorCodesRepository.findAll();
    }

    @Cacheable(value = "errorCode", key = "#errorCodeId")
    @Override
    public ErrorCodes getErrorCode(String errorCodeId){
        return errorCodesRepository.findErrorCodesById(errorCodeId)
                .orElseThrow(() -> new ErrorCodesNotFoundException(errorCodeId, LocalDateTime.now()));
    }

    @CacheEvict(value = {"errorCodes","errorCode"}, allEntries = true)
    public void refreshErrorCodesCache(){
        log.info("Error Codes Cache has been refreshed");
    }
}
