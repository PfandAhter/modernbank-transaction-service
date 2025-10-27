package com.modernbank.transaction_service.service.event;

import com.modernbank.transaction_service.api.request.TransferMoneyATMRequest;
import com.modernbank.transaction_service.api.request.WithdrawFromATMRequest;
import com.modernbank.transaction_service.api.response.BaseResponse;

public interface IWithdrawFromATMServiceProducer {
    BaseResponse transferMoneyATM(TransferMoneyATMRequest request);

    BaseResponse withdrawMoneyFromATM(WithdrawFromATMRequest request);
}
