package com.modernbank.transaction_service.service;

import com.modernbank.transaction_service.entity.ErrorCodes;

public interface ErrorCacheService {

    ErrorCodes getErrorCodeByErrorId(String code);

    void refreshAllErrorCodesCache();
}