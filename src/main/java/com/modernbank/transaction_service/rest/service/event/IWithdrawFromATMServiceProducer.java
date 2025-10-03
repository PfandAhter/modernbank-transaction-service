package com.modernbank.transaction_service.rest.service.event;

import com.modernbank.transaction_service.rest.controller.request.TransferMoneyATMRequest;
import com.modernbank.transaction_service.rest.controller.request.WithdrawFromATMRequest;
import com.modernbank.transaction_service.rest.controller.response.BaseResponse;

public interface IWithdrawFromATMServiceProducer {
    BaseResponse transferMoneyATM(TransferMoneyATMRequest request);

    BaseResponse withdrawMoneyFromATM(WithdrawFromATMRequest request);
}
