package com.modernbank.transaction_service.rest.service;

import com.modernbank.transaction_service.model.entity.ErrorCodes;

import java.util.List;

public interface IErrorCacheService {

    List<ErrorCodes> getAllErrorCodes();

    ErrorCodes getErrorCode(String errorCodeId);

    void refreshErrorCodesCache();
}
