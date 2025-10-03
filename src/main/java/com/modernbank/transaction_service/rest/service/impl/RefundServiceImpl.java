package com.modernbank.transaction_service.rest.service.impl;

import com.modernbank.transaction_service.api.client.AccountServiceClient;
import com.modernbank.transaction_service.api.client.NotificationServiceClient;
import com.modernbank.transaction_service.api.request.SendNotificationRequest;
import com.modernbank.transaction_service.api.response.GetAccountByIban;
import com.modernbank.transaction_service.model.entity.ATMTransfer;
import com.modernbank.transaction_service.rest.service.IRefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundServiceImpl implements IRefundService {

    private final AccountServiceClient accountServiceClient;

    private final NotificationServiceClient notificationServiceClient;

    @Override
    public void refundMoneyToAccountFromATM(ATMTransfer atmTransfer) {
        log.info("Refunding money to account from ATM");
        accountServiceClient.updateBalance(atmTransfer.getSenderIban(),atmTransfer.getAmount());
        GetAccountByIban senderAccount = accountServiceClient.getAccountByIban(atmTransfer.getSenderIban());

        notificationServiceClient.sendNotification(SendNotificationRequest.builder()
                .title("Refund")
                .type("info")
                .userId(senderAccount.getUserId())
                .message("You have received a refund of " + atmTransfer.getAmount() + " from ATM with ID: " + atmTransfer.getAtmId())
                .build());
        log.info("Refunded money to account from ATM");
    }
}