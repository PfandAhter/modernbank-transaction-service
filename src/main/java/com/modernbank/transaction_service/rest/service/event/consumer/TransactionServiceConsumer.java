package com.modernbank.transaction_service.rest.service.event.consumer;


import com.modernbank.transaction_service.api.client.AccountServiceClient;
import com.modernbank.transaction_service.api.request.SendNotificationRequest;
import com.modernbank.transaction_service.api.response.GetAccountByIban;
import com.modernbank.transaction_service.exception.NotFoundException;
import com.modernbank.transaction_service.rest.controller.request.TransferMoneyRequest;
import com.modernbank.transaction_service.rest.controller.request.WithdrawAndDepositMoneyRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.modernbank.transaction_service.constant.ErrorCodeConstants.INSUFFICIENT_FUNDS;

@Service
@RequiredArgsConstructor
@Slf4j

public class TransactionServiceConsumer {

    private final KafkaTemplate<String, WithdrawAndDepositMoneyRequest> withdrawAndDepositMoneyKafkaTemplate;

    private final KafkaTemplate<String, TransferMoneyRequest> transferMoneyKafkaTemplate;

    private final KafkaTemplate<String, SendNotificationRequest> notificationKafkaTemplate;

    private final AccountServiceClient accountServiceClient;

    @KafkaListener(topics = "${kafka.topics.withdraw-money}", groupId = "${spring.application.name}")
    public void consumeWithdrawMoney(WithdrawAndDepositMoneyRequest request) {
        log.info("Received withdraw and deposit money request: {}", request);

        // transactionService.processTransaction(request);
    }

    @KafkaListener(topics = "${kafka.topics.deposit-money}", groupId = "${spring.application.name}")
    public void consumeDepositMoney(WithdrawAndDepositMoneyRequest request) {
        log.info("Received withdraw and deposit money request: {}", request);

        // transactionService.processTransaction(request);
    }

    //Money Transfer Process

    @KafkaListener(topics = "${kafka.topics.transfer-money}", groupId = "transfer-group")
    public void processStartTransferMoney(TransferMoneyRequest request) {
        log.info("Received withdraw and deposit money request: {}", request);
        try {
            //TODO : EN SON BURADAYDIM... Burada kullanicinin hesabinin ibanini gonderdigi iban adresini de kontrol etmemzi gerekiyor. Burada
            //TODO gonderecegi iban adresinin isim bilgilerini de teyit etmemiz gerekmektedir. Ondan sonrasi burada basliyor...
            GetAccountByIban senderAccountByIban = accountServiceClient.getAccountByIban(request.getFromAccountId());
            GetAccountByIban receiverAccountByIban = accountServiceClient.getAccountByIban(request.getToAccountId());

            if(!senderAccountByIban.getCurrency().equals(receiverAccountByIban.getCurrency())){
                log.info("Currency is not same -> accountId: " + request.getFromAccountId());
                throw new NotFoundException("Currency is not same");
            }

            if(!receiverAccountByIban.getFirstName().equals(request.getToFirstName()) &&
                    !receiverAccountByIban.getSecondName().equals(request.getToSecondName()) &&
                    !receiverAccountByIban.getLastName().equals(request.getToLastName())){
                log.info("Account name is not same -> accountId: " + request.getToAccountId());
                throw new NotFoundException("Account name is not same");
            }

            if (senderAccountByIban.getBalance() >= request.getAmount()) {
                accountServiceClient.updateBalance(request.getFromAccountId(), -request.getAmount());

                // Kafka'ya mesaj gönder
                transferMoneyKafkaTemplate.send("${kafka.topics.transfer-update-money}", request);
            } else {
                log.info("Insufficient funds -> accountId: " + request.getFromAccountId());
                throw new NotFoundException(INSUFFICIENT_FUNDS);
            }
        } catch (RuntimeException exception) {
            log.warn("Error at transfer start : ", exception.getMessage());

            /*dltKafkaTemplate.send("money-process.DLT", MoneyProcessFailed.builder()
                    .errorCode(exception.getMessage())
                    .bankName("BakirBank")
                    .amount(transferRequest.getAmount())
                    .customerId(transferRequest.getFromId())
                    .transactionType(TransactionType.TRANSFER).build());*/
        }
    }

    @KafkaListener(topics = "${kafka.topics.transfer-update-money}", groupId = "transfer-group")
    public void processUpdateTransferMoney(TransferMoneyRequest request){

        try {
            accountServiceClient.updateBalance(request.getToAccountId(), request.getAmount());

            // Send finalize message to kafka
            transferMoneyKafkaTemplate.send("${kafka.topics.transfer-finalize-money}", request);
        } catch (Exception notFoundException) {
            log.error("Error at transfer update : ", notFoundException.getMessage());

            accountServiceClient.updateBalance(request.getFromAccountId(), request.getAmount());

            /*dltKafkaTemplate.send("money-process.DLT", MoneyProcessFailed.builder()
                    .errorCode(notFoundException.getMessage())
                    .bankName("BakirBank")
                    .amount(transferRequest.getAmount())
                    .customerId(transferRequest.getFromId())
                    .transactionType(TransactionType.TRANSFER).build());*/

            //throw new NotFoundException(notFoundException.getMessage());
        }
    }

    @KafkaListener(topics = "${kafka.topics.transfer-finalize-money}", groupId = "transfer-group")
    public void processFinalizeTransferMoney(TransferMoneyRequest request){
        //notficationservice or emailservice call...
        try {
            GetAccountByIban receiver = accountServiceClient.getAccountByIban(request.getToAccountId());
            GetAccountByIban sender = accountServiceClient.getAccountByIban(request.getFromAccountId());

            if(receiver == null || sender == null) {
                log.info("Account not found -> accountId: " + request.getFromAccountId());
                throw new NotFoundException("Account not found");
            }

            String receiverNotificationMessage = String.format(
                    "Dear customer %s, your transfer from %s amount %.2f is successful.",
                    receiver.getFirstName(),
                    sender.getFirstName() + " " + sender.getLastName(),
                    request.getAmount()
            );

            String senderNotificationMessage = String.format(
                    "Dear customer %s, your transfer to %s amount %.2f is successful.",
                    sender.getFirstName(),
                    receiver.getFirstName() + " " + receiver.getLastName(),
                    request.getAmount()
            );

            notificationKafkaTemplate.send("notification-service", SendNotificationRequest.builder()
                    .userId(receiver.getUserId())
                    .message(receiverNotificationMessage)
                    .build());

            notificationKafkaTemplate.send("notification-service", SendNotificationRequest.builder()
                    .userId(sender.getUserId())
                    .message(senderNotificationMessage)
                    .build());


            /*transactionRepository.save(Transaction.builder()
                    .senderAccount(sender)
                    .receiverAccount(receiver)
                    .transactionDate(LocalDateTime.now())
                    .description(transferRequest.getDescription())
                    .amount(transferRequest.getAmount())
                    .transactionType(TransactionType.TRANSFER).build());*/
            //INVOICE SERVICE MAY CALL HERE CREATE INVOICE SERVICE
            //SEND THIS MESSAGES TO NOTIFICATION AND EMAIL SERVICE
            //EMAIL SERVICE WILL SEND WITH HIM SHOPPING CART INFORMATION INCLUDES PRODUCTS PHOTOS EMAIL TO CUSTOMERS //Bunu stock serviceden felan da yapabiliriz


        } catch (Exception exception) {
            log.error("Error at transfer finalize :  ", exception.getMessage());

            /*dltKafkaTemplate.send("money-process.DLT", MoneyProcessFailed.builder()
                    .errorCode(exception.getMessage())
                    .bankName("BakirBank")
                    .amount(transferRequest.getAmount())
                    .customerId(transferRequest.getFromId())
                    .transactionType(TransactionType.TRANSFER).build());*/
        }
    }
}