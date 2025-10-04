package com.modernbank.transaction_service.rest.service;

import com.modernbank.transaction_service.entity.ATMTransfer;

public interface IRefundService {

    void refundMoneyToAccountFromATM(ATMTransfer atmTransfer);
}