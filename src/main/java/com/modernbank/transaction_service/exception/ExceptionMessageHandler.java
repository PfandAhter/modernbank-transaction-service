package com.modernbank.transaction_service.exception;

import com.modernbank.transaction_service.api.dto.ErrorCodesDTO;
import com.modernbank.transaction_service.rest.service.IErrorCacheService;
import com.modernbank.transaction_service.rest.service.IMapperService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExceptionMessageHandler {

    private final IErrorCacheService errorCacheService;

    private final IMapperService mapperService;


    public String createFailResponseBody(String errorCode){
        ErrorCodesDTO errorCodesDTO = findByErrorCode(errorCode);
        return errorCodesDTO.getDescription();
    }

    private ErrorCodesDTO findByErrorCode(String errorId){
        return mapperService.map(errorCacheService.getErrorCode(errorId), ErrorCodesDTO.class);
    }

}