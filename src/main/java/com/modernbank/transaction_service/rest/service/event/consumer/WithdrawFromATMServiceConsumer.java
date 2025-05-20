package com.modernbank.transaction_service.rest.service.event.consumer;

import com.modernbank.transaction_service.api.client.ATMReportingServiceClient;
import com.modernbank.transaction_service.api.client.AccountServiceClient;
import com.modernbank.transaction_service.api.client.NotificationServiceClient;
import com.modernbank.transaction_service.api.request.SendNotificationRequest;
import com.modernbank.transaction_service.api.response.GetAccountByIban;
import com.modernbank.transaction_service.api.response.GetATMNameAndIDResponse;
import com.modernbank.transaction_service.exception.ExceptionMessageHandler;
import com.modernbank.transaction_service.exception.NotFoundException;
import com.modernbank.transaction_service.exception.ProcessFailedException;
import com.modernbank.transaction_service.model.entity.ATMTransfer;
import com.modernbank.transaction_service.model.enums.ATMTransferStatus;
import com.modernbank.transaction_service.repository.ATMTransferRepository;
import com.modernbank.transaction_service.rest.controller.request.TransferMoneyATMRequest;
import com.modernbank.transaction_service.rest.controller.request.WithdrawFromATMRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import static com.modernbank.transaction_service.constant.ErrorCodeConstants.INSUFFICIENT_FUNDS;
import static com.modernbank.transaction_service.constant.ErrorCodeConstants.RECEIVER_ACCOUNT_NOT_MATCH;
import static com.modernbank.transaction_service.constant.ErrorCodeConstants.ATM_TRANSFER_NOT_FOUND_BY_IBAN;
import static com.modernbank.transaction_service.constant.ErrorCodeConstants.ATM_TRANSFER_NOT_FOUND_BY_TCKN;
import static com.modernbank.transaction_service.constant.ErrorCodeConstants.ATM_TRANSFER_NOT_FOUND_BY_IBAN_OR_ATMID;
import static com.modernbank.transaction_service.constant.ErrorCodeConstants.ATM_TRANSFER_NOT_FOUND_BY_TCKN_OR_ATMID;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j

public class WithdrawFromATMServiceConsumer {

    private final AccountServiceClient accountServiceClient;

    private final ATMTransferRepository atmTransferRepository;

    private final NotificationServiceClient notificationServiceClient;

    private final ATMReportingServiceClient atmReportingServiceClient;

    private final ExceptionMessageHandler exceptionMessageHandler;

    @KafkaListener(topics = "deposit-money-to-atm", groupId = "transfer-money-to-atm-group", containerFactory = "transferMoneyToATMKafkaListenerContainerFactory")
    public void consumeDepositMoney(TransferMoneyATMRequest request) {
        log.info("Received deposit money to atm request: {}", request);
        GetAccountByIban senderAccountInfo = accountServiceClient.getAccountByIban(request.getSenderIban());

        try{
            if(senderAccountInfo.getBalance() < request.getAmount()){
                log.info("Insufficient funds for account: {}", request.getSenderIban());
                throw new ProcessFailedException(INSUFFICIENT_FUNDS);
                // Call Notification Service...
            }

            if (request.getReceiverTckn() == null || request.getReceiverTckn().length() != 11){
                GetAccountByIban receiverAccountInfo = accountServiceClient.getAccountByIban(request.getReceiverIban());

                if(receiverAccountInfo.getSecondName() == null){
                    receiverAccountInfo.setSecondName("");
                }

                if (!(request.getReceiverFirstName().equals(receiverAccountInfo.getFirstName())) ||
                        !(request.getReceiverSecondName().equals(receiverAccountInfo.getSecondName())) ||
                        !(request.getReceiverLastName().equals(receiverAccountInfo.getLastName()))) {
                    log.info("Receiver account information does not match");
                    throw new ProcessFailedException(RECEIVER_ACCOUNT_NOT_MATCH);
                    // Call Notification Service...
                }

                atmTransferRepository.save(ATMTransfer.builder()
                        .atmId(request.getAtmId())
                        .senderIban(request.getSenderIban())
                        .receiverIban(request.getReceiverIban())
                        .senderFirstName(request.getSenderFirstName())
                        .senderSecondName(request.getSenderSecondName())
                        .senderLastName(request.getSenderLastName())
                        .receiverFirstName(request.getReceiverFirstName())
                        .receiverSecondName(request.getReceiverSecondName())
                        .receiverLastName(request.getReceiverLastName())
                        .amount(request.getAmount())
                        .transferDate(LocalDateTime.now())
                        .updateDate(LocalDateTime.now())
                        .active(1)
                        .status(ATMTransferStatus.PENDING)
                        .description(request.getDescription())
                        .build());

            }else{
                atmTransferRepository.save(ATMTransfer.builder()
                        .atmId(request.getAtmId())
                        .senderIban(request.getSenderIban())
                        .receiverTckn(request.getReceiverTckn())
                        .senderFirstName(request.getSenderFirstName())
                        .senderSecondName(request.getSenderSecondName())
                        .senderLastName(request.getSenderLastName())
                        .amount(request.getAmount())
                        .active(1)
                        .status(ATMTransferStatus.PENDING)
                        .transferDate(LocalDateTime.now())
                        .updateDate(LocalDateTime.now())
                        .description(request.getDescription())
                        .build());
            }

            accountServiceClient.updateBalance(request.getSenderIban(), -request.getAmount());
            //transactionRepository.save(Transaction.builder()

            notificationServiceClient.sendNotification(SendNotificationRequest.builder()
                            .userId(senderAccountInfo.getUserId())
                            .title("Deposit Money to ATM")
                            .type("info")
                            .message(String.format("You have successfully deposited %.2f to ATM with ID %s", request.getAmount(), request.getAtmId()))
                            .build());

        }catch (Exception e){
            log.error("Error occurred while processing deposit money to atm request: {}", e.getMessage());

            notificationServiceClient.sendNotification(SendNotificationRequest.builder()
                            .userId(senderAccountInfo.getUserId())
                            .title("Deposit Money to ATM")
                            .type("error")
                            .message(String.format("Failed to deposit %.2f to ATM with ID %s. Reason: %s", request.getAmount(), request.getAtmId(), exceptionMessageHandler.createFailResponseBody(e.getMessage())))
                            .build());

        }
    }

    @KafkaListener(topics = "withdraw-money-from-atm", groupId = "withdraw-money-from-atm-group", containerFactory = "withdrawFromATMKafkaListenerContainerFactory")
    public void withdrawMoneyFromATM(WithdrawFromATMRequest request){
        try{
            List<ATMTransfer> atmTransfersOptional;

            if(request.getTckn() == null || request.getTckn().length() != 11) {
                atmTransfersOptional = atmTransferRepository.findATMTransferByReceiverIbanOrReceiverTcknAndActive(request.getIban(), request.getAtmId(),ATMTransferStatus.PENDING);

                if(atmTransfersOptional == null){
                    throw new NotFoundException(ATM_TRANSFER_NOT_FOUND_BY_IBAN_OR_ATMID);
                }

                boolean anyMatch = atmTransfersOptional.stream()
                        .anyMatch(atmTransfer -> atmTransfer.getReceiverIban().equals(request.getIban()));
                if(!anyMatch) {
                    throw new NotFoundException(ATM_TRANSFER_NOT_FOUND_BY_IBAN);
                }

                atmTransfersOptional.stream()
                        .forEach(atmTransfer -> {
                            atmTransfer.setActive(0);
                            atmTransfer.setUpdateDate(LocalDateTime.now());
                            atmTransfer.setStatus(ATMTransferStatus.COMPLETED);
                            atmTransferRepository.save(atmTransfer);
                        });
            }else{
                atmTransfersOptional = atmTransferRepository.findATMTransferByReceiverIbanOrReceiverTcknAndActive(request.getTckn(), request.getAtmId(), ATMTransferStatus.PENDING);

                if(atmTransfersOptional == null){
                    throw new NotFoundException(ATM_TRANSFER_NOT_FOUND_BY_TCKN_OR_ATMID);
                }
                boolean anyMatch = atmTransfersOptional.stream()
                        .anyMatch(atmTransfer -> atmTransfer.getReceiverTckn().equals(request.getTckn()));

                if (!anyMatch) {
                    throw new NotFoundException(ATM_TRANSFER_NOT_FOUND_BY_TCKN);
                }

                atmTransfersOptional.stream()
                        .forEach(atmTransfer ->{
                            atmTransfer.setActive(0);
                            atmTransfer.setUpdateDate(LocalDateTime.now());
                            atmTransfer.setStatus(ATMTransferStatus.COMPLETED);
                            atmTransferRepository.save(atmTransfer);
                        });
            }
            GetAccountByIban accountByIban = accountServiceClient.getAccountByIban(atmTransfersOptional.get(0).getSenderIban());
            GetATMNameAndIDResponse atmInfo = atmReportingServiceClient.getATMById(request.getAtmId());

            atmTransfersOptional.stream()
                    .forEach(atmTransfer -> {
                        notificationServiceClient.sendNotification(SendNotificationRequest.builder()
                                        .userId(accountByIban.getUserId())
                                        .title("Withdraw Money From ATM")
                                        .type("info")
                                        .message(String.format("The %.2f TL you sent to the %s ATM was withdrawn by its owner.",atmTransfer.getAmount(), atmInfo.getName()))
                                .build());
                    });

        }catch (Exception e){
            log.error("Error occurred while processing withdraw money from atm request: {}", e.getMessage());
            // Call Notification Service...
        }
    }
}