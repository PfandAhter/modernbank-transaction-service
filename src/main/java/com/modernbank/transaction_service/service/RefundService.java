package com.modernbank.transaction_service.service;

import com.modernbank.transaction_service.entity.ATMTransfer;

public interface RefundService {

    void refundMoneyToAccountFromATM(ATMTransfer atmTransfer);
}