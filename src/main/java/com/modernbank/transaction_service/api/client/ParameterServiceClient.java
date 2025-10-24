package com.modernbank.transaction_service.api.client;

import com.modernbank.transaction_service.api.request.LogErrorRequest;
import com.modernbank.transaction_service.api.response.BaseResponse;
import com.modernbank.transaction_service.api.response.GetAllErrorCodesResponse;
import com.modernbank.transaction_service.api.response.GetErrorCodeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "parameter-service", url = "${feign.client.parameter-service.url}")
public interface ParameterServiceClient {

    @GetMapping(path = "${feign.client.parameter-service.getByCode}")
    GetErrorCodeResponse getErrorCode(@RequestParam(value = "code") String errorCodeId);

    @PostMapping(value = "${feign.client.parameter-service.logError}")
    BaseResponse logError(@RequestBody LogErrorRequest request);

    @GetMapping(value = "${feign.client.parameter-service.getAllErrorCodes}")
    GetAllErrorCodesResponse getAllErrorCodes(@RequestParam("name") String serviceName);
}