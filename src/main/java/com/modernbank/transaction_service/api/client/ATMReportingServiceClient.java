package com.modernbank.transaction_service.api.client;

import com.modernbank.transaction_service.api.response.GetATMNameAndIDResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "atm-reporting-service", url = "${feign.client.atm-reporting-service.url}")
public interface ATMReportingServiceClient {

    @GetMapping(path = "${feign.client.atm-reporting-service.getATMById}")
    GetATMNameAndIDResponse getATMById(@RequestParam("id") String atmId);
}