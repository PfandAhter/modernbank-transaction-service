package com.modernbank.transaction_service.api.client;


import com.modernbank.transaction_service.api.response.GetAccountByIban;
import com.modernbank.transaction_service.rest.controller.response.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "account-service", url = "${feign.client.account-service.url}")
public interface AccountServiceClient {

    @GetMapping(path = "${feign.client.account-service.extractFromIBAN}")
    GetAccountByIban getAccountByIban(@RequestParam(value = "iban") String iban);

    @PostMapping(path = "${feign.client.account-service.updateBalance}")
    BaseResponse updateBalance(@RequestParam(value = "iban") String iban,
                    @RequestParam(value = "balance") double balance);

}
