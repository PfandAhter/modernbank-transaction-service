package com.modernbank.transaction_service.service.event.consumer;

import com.modernbank.transaction_service.api.client.ATMReportingServiceClient;
import com.modernbank.transaction_service.api.client.AccountServiceClient;
import com.modernbank.transaction_service.api.client.NotificationServiceClient;
import com.modernbank.transaction_service.api.request.SendNotificationRequest;
import com.modernbank.transaction_service.api.response.GetAccountByIban;
import com.modernbank.transaction_service.api.response.GetATMNameAndIDResponse;
import com.modernbank.transaction_service.exception.NotFoundException;
import com.modernbank.transaction_service.exception.ProcessFailedException;
import com.modernbank.transaction_service.entity.ATMTransfer;
import com.modernbank.transaction_service.entity.Transaction;
import com.modernbank.transaction_service.model.enums.*;
import com.modernbank.transaction_service.repository.ATMTransferRepository;
import com.modernbank.transaction_service.repository.TransactionRepository;
import com.modernbank.transaction_service.api.request.TransferMoneyATMRequest;
import com.modernbank.transaction_service.api.request.WithdrawFromATMRequest;
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

    private final TransactionRepository transactionRepository;

    @KafkaListener(topics = "deposit-money-to-atm", groupId = "transfer-money-to-atm-group", containerFactory = "transferMoneyToATMKafkaListenerContainerFactory")
    public void consumeDepositMoney(TransferMoneyATMRequest request) {
        log.info("Received deposit money to atm request: {}", request);
        GetAccountByIban senderAccountInfo = accountServiceClient.getAccountByIban(request.getSenderIban());

        ATMTransfer atmTransfer;

        try {
            if (senderAccountInfo.getBalance() < request.getAmount()) {
                log.info("Insufficient funds for account: {}", request.getSenderIban());
                throw new ProcessFailedException(INSUFFICIENT_FUNDS);
            }

            if (request.getReceiverTckn() == null || request.getReceiverTckn().length() != 11) {
                GetAccountByIban receiverAccountInfo = accountServiceClient.getAccountByIban(request.getReceiverIban());

                if (receiverAccountInfo.getSecondName() == null) {
                    receiverAccountInfo.setSecondName("");
                }

                if (!(request.getReceiverFirstName().equals(receiverAccountInfo.getFirstName())) ||
                        !(request.getReceiverSecondName().equals(receiverAccountInfo.getSecondName())) ||
                        !(request.getReceiverLastName().equals(receiverAccountInfo.getLastName()))) {
                    log.info("Receiver account information does not match");
                    throw new ProcessFailedException(RECEIVER_ACCOUNT_NOT_MATCH); //TODO: Bunu test et...
                }

                atmTransfer = ATMTransfer.builder()
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
                        .build();

            } else {
                atmTransfer = ATMTransfer.builder()
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
                        .build();
            }

            accountServiceClient.updateBalance(request.getSenderIban(), -request.getAmount());

            Transaction transaction = Transaction.builder()
                    .accountId(senderAccountInfo.getAccountId())
                    .amount(request.getAmount())
                    .senderFirstName(request.getSenderFirstName())
                    .senderSecondName(request.getSenderSecondName())
                    .senderLastName(request.getSenderLastName())
                    .receiverFirstName(request.getReceiverFirstName())
                    .receiverSecondName(request.getReceiverSecondName())
                    .receiverLastName(request.getReceiverLastName())
                    .receiverTckn(request.getReceiverTckn())
                    .receiverIban(request.getReceiverIban())
                    .type(TransactionType.EXPENSE)
                    .channel(TransactionChannel.ONLINE_BANKING)
                    .category(TransactionCategory.ATM_DEPOSIT)
                    .status(TransactionStatus.PENDING)
                    .description(request.getDescription())
                    .updatedDate(LocalDateTime.now())
                    .date(LocalDateTime.now())
                    .build();

            atmTransfer.setTransactionId(transaction.getId());

            atmTransferRepository.save(atmTransfer);
            transactionRepository.save(transaction);

            GetATMNameAndIDResponse atmInfo = atmReportingServiceClient.getATMById(request.getAtmId());

            notificationServiceClient.sendNotification(SendNotificationRequest.builder()
                    .userId(senderAccountInfo.getUserId())
                    .title("Deposit Money to ATM")
                    .type("info")
                    .message(String.format("You have successfully deposited %.2f to ATM with Name %s", request.getAmount(), atmInfo.getName()))
                    .build());

        } catch (Exception e) {
            log.error("Error occurred while processing deposit money to atm request: {}", e.getMessage());
            GetATMNameAndIDResponse atmInfo = atmReportingServiceClient.getATMById(request.getAtmId());

            notificationServiceClient.sendNotification(SendNotificationRequest.builder()
                    .userId(senderAccountInfo.getUserId())
                    .title("Deposit Money to ATM")
                    .type("error")
                    .message(String.format("Failed to deposit %.2f to ATM with Name %s.", request.getAmount(), atmInfo.getName()))
                    .build());

        }
    }

    @KafkaListener(topics = "withdraw-money-from-atm", groupId = "withdraw-money-from-atm-group", containerFactory = "withdrawFromATMKafkaListenerContainerFactory")
    public void withdrawMoneyFromATM(WithdrawFromATMRequest request) {
        try {
            List<ATMTransfer> atmTransfersOptional;

            if (request.getTckn() == null || request.getTckn().length() != 11) {
                atmTransfersOptional = atmTransferRepository.findATMTransferByReceiverIbanOrReceiverTcknAndActive(request.getIban(), request.getAtmId(), ATMTransferStatus.PENDING);

                if (atmTransfersOptional == null) {
                    throw new NotFoundException(ATM_TRANSFER_NOT_FOUND_BY_IBAN_OR_ATMID);
                }

                boolean anyMatch = atmTransfersOptional.stream()
                        .anyMatch(atmTransfer -> atmTransfer.getReceiverIban().equals(request.getIban()));
                if (!anyMatch) {
                    throw new NotFoundException(ATM_TRANSFER_NOT_FOUND_BY_IBAN);
                }

                atmTransfersOptional.stream()
                        .forEach(atmTransfer -> {
                            atmTransfer.setActive(0);
                            atmTransfer.setUpdateDate(LocalDateTime.now());
                            atmTransfer.setStatus(ATMTransferStatus.COMPLETED);
                            atmTransferRepository.save(atmTransfer);
                        });
            } else {
                atmTransfersOptional = atmTransferRepository.findATMTransferByReceiverIbanOrReceiverTcknAndActive(request.getTckn(), request.getAtmId(), ATMTransferStatus.PENDING);

                if (atmTransfersOptional == null) {
                    throw new NotFoundException(ATM_TRANSFER_NOT_FOUND_BY_TCKN_OR_ATMID);
                }
                boolean anyMatch = atmTransfersOptional.stream()
                        .anyMatch(atmTransfer -> atmTransfer.getReceiverTckn().equals(request.getTckn()));

                if (!anyMatch) {
                    throw new NotFoundException(ATM_TRANSFER_NOT_FOUND_BY_TCKN);
                }

                atmTransfersOptional.stream()
                        .forEach(atmTransfer -> {
                            atmTransfer.setActive(0);
                            atmTransfer.setUpdateDate(LocalDateTime.now());
                            atmTransfer.setStatus(ATMTransferStatus.COMPLETED);
                            atmTransferRepository.save(atmTransfer);
                        });
            }
            GetATMNameAndIDResponse atmInfo = atmReportingServiceClient.getATMById(request.getAtmId());

            atmTransfersOptional.stream()
                    .collect(java.util.stream.Collectors.groupingBy(ATMTransfer::getSenderIban))
                    .forEach((senderIban, transfers) -> {
                        double totalAmount = transfers.stream().mapToDouble(ATMTransfer::getAmount).sum();
                        GetAccountByIban accountByIban = accountServiceClient.getAccountByIban(senderIban);
                        int transactionCount = transfers.size();

                        transfers.forEach(atmTransfer -> {
                            if(atmTransfer.getAtmId().equals(request.getAtmId())){
                                Transaction transaction = transactionRepository.findTransactionByAccountId(accountByIban.getAccountId(), atmTransfer.getTransactionId());
                                transaction.setStatus(TransactionStatus.COMPLETED);
                                transaction.setTitle(String.format("Withdrawn %.2f TL from ATM with name %s", atmTransfer.getAmount(), atmInfo.getName()));
                                transaction.setDescription(atmTransfer.getDescription());
                                transaction.setUpdatedDate(LocalDateTime.now());


                                transactionRepository.save(transaction);
                            }
                        });

                        //Burada hangi ATMden cekildiginin kontrolu yapilmasi gerekiyor...

                        /*transfers.forEach(atmTransfer -> {
                            transactionRepository.save(Transaction.builder()
                                    .accountId(accountByIban.getAccountId())
                                    .amount(atmTransfer.getAmount())
                                    .senderFirstName(atmTransfer.getSenderFirstName())
                                    .senderSecondName(atmTransfer.getSenderSecondName())
                                    .senderLastName(atmTransfer.getSenderLastName())
                                    .receiverFirstName(atmTransfer.getReceiverFirstName())
                                    .receiverSecondName(atmTransfer.getReceiverSecondName())
                                    .receiverLastName(atmTransfer.getReceiverLastName())
                                    .receiverIban(atmTransfer.getReceiverIban())
                                    .receiverTckn(atmTransfer.getReceiverTckn())
                                    .type(TransactionType.EXPENSE)
                                    .channel(TransactionChannel.ATM)
                                    .category(TransactionCategory.ATM_WITHDRAWAL)
                                    .status(TransactionStatus.COMPLETED)
                                    .date(LocalDateTime.now())
                                    .description(String.format("Withdrawn %.2f TL from ATM with ID %s. Description: %s", atmTransfer.getAmount(), request.getAtmId(), atmTransfer.getDescription()))
                                    .build());
                        });*/


                        notificationServiceClient.sendNotification(SendNotificationRequest.builder()
                                .userId(accountServiceClient.getAccountByIban(senderIban).getUserId())
                                .title("Withdraw Money From ATM")
                                .type(TransactionType.EXPENSE.getTransactionType())
                                .message(String.format("Sender IBAN %s made %d transactions totaling %.2f TL withdrawn from %s ATM.", senderIban, transactionCount, totalAmount, atmInfo.getName()))
                                .build());
                    });

        } catch (Exception e) {
            log.error("Error occurred while processing withdraw money from atm request: {}", e.getMessage());
            // Call Notification Service...
        }
    }
}