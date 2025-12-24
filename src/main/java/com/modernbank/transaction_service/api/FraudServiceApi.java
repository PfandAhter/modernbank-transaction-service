package com.modernbank.transaction_service.api;

import com.modernbank.transaction_service.api.request.TransactionAdditionalApproveRequest;
import com.modernbank.transaction_service.api.response.BaseResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface FraudServiceApi {

    @PostMapping("/transaction/additional/approval")
    BaseResponse requestAdditionalApproval(@RequestBody TransactionAdditionalApproveRequest request);
}
