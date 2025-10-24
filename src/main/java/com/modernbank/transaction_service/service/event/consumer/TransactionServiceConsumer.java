package com.modernbank.transaction_service.service.event.consumer;


import com.modernbank.transaction_service.api.client.AccountServiceClient;
import com.modernbank.transaction_service.api.request.ChatNotificationRequest;
import com.modernbank.transaction_service.api.request.SendNotificationRequest;
import com.modernbank.transaction_service.api.response.GetAccountByIban;
import com.modernbank.transaction_service.entity.Transaction;
import com.modernbank.transaction_service.exception.NotFoundException;
import com.modernbank.transaction_service.api.request.TransferMoneyRequest;
import com.modernbank.transaction_service.api.request.WithdrawAndDepositMoneyRequest;
import com.modernbank.transaction_service.model.enums.TransactionCategory;
import com.modernbank.transaction_service.model.enums.TransactionChannel;
import com.modernbank.transaction_service.model.enums.TransactionStatus;
import com.modernbank.transaction_service.model.enums.TransactionType;
import com.modernbank.transaction_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;

import static com.modernbank.transaction_service.constant.ErrorCodeConstants.INSUFFICIENT_FUNDS;
import static com.modernbank.transaction_service.constant.ErrorCodeConstants.ACCOUNT_NOT_FOUND;
import static com.modernbank.transaction_service.constant.ErrorCodeConstants.CURRENCY_IS_NOT_SAME;
import static com.modernbank.transaction_service.constant.ErrorCodeConstants.ACCOUNT_NAME_IS_NOT_SAME;

@Service
@RequiredArgsConstructor
@Slf4j

public class TransactionServiceConsumer {

    //private final KafkaTemplate<String, WithdrawAndDepositMoneyRequest> withdrawAndDepositMoneyKafkaTemplate;

    private final KafkaTemplate<String, TransferMoneyRequest> transferMoneyKafkaTemplate;

    private final KafkaTemplate<String, SendNotificationRequest> notificationKafkaTemplate;

    private final KafkaTemplate<String, ChatNotificationRequest> chatNotificationKafkaTemplate;

    private final AccountServiceClient accountServiceClient;

    private final TransactionRepository transactionRepository;

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

    @KafkaListener(topics = "start-transfer-money", groupId = "transfer-group", containerFactory = "moneyTransferKafkaListenerContainerFactory")
    public void processStartTransferMoney(TransferMoneyRequest request) {
        log.info("Received withdraw and deposit money request: {}", request);
        try {
            //TODO : EN SON BURADAYDIM... Burada kullanicinin hesabinin ibanini gonderdigi iban adresini de kontrol etmemzi gerekiyor. Burada
            //TODO gonderecegi iban adresinin isim bilgilerini de teyit etmemiz gerekmektedir. Ondan sonrasi burada basliyor...
            GetAccountByIban senderAccountByIban = accountServiceClient.getAccountByIban(request.getFromIBAN());
            GetAccountByIban receiverAccountByIban = accountServiceClient.getAccountByIban(request.getToIBAN());

            if (!senderAccountByIban.getCurrency().equals(receiverAccountByIban.getCurrency())) {
                log.info("Currency is not same -> accountId: " + request.getFromIBAN());
                throw new NotFoundException(CURRENCY_IS_NOT_SAME);
            }

            if (!receiverAccountByIban.getFirstName().equals(request.getToFirstName()) &&
                    !receiverAccountByIban.getSecondName().equals(request.getToSecondName()) &&
                    !receiverAccountByIban.getLastName().equals(request.getToLastName())) {
                log.info("Account name is not same -> accountId: " + request.getToIBAN());
                throw new NotFoundException(ACCOUNT_NAME_IS_NOT_SAME);
            }

            if (senderAccountByIban.getBalance() >= request.getAmount()) {
                accountServiceClient.updateBalance(request.getFromIBAN(), -request.getAmount());

                // Kafka'ya mesaj gönder
                transferMoneyKafkaTemplate.send("update-transfer-money", request);
            } else {
                log.info("Insufficient funds -> accountId: " + request.getFromIBAN());
                throw new NotFoundException(INSUFFICIENT_FUNDS);
            }
        } catch (RuntimeException exception) {
            log.warn("Error at transfer start : ", exception.getMessage());

            throw new NotFoundException(exception.getMessage());
            /*dltKafkaTemplate.send("money-process.DLT", MoneyProcessFailed.builder()
                    .errorCode(exception.getMessage())
                    .bankName("BakirBank")
                    .amount(transferRequest.getAmount())
                    .customerId(transferRequest.getFromId())
                    .transactionType(TransactionType.TRANSFER).build());*/
        }
    }

    @KafkaListener(topics = "update-transfer-money", groupId = "transfer-group", containerFactory = "moneyTransferKafkaListenerContainerFactory")
    public void processUpdateTransferMoney(TransferMoneyRequest request) {

        try {
            accountServiceClient.updateBalance(request.getToIBAN(), request.getAmount());

            // Send finalize message to kafka
            transferMoneyKafkaTemplate.send("finalize-transfer-money", request);
        } catch (Exception notFoundException) {
            log.error("Error at transfer update : ", notFoundException.getMessage());

            accountServiceClient.updateBalance(request.getFromIBAN(), request.getAmount());

            throw new NotFoundException(notFoundException.getMessage());
            /*dltKafkaTemplate.send("money-process.DLT", MoneyProcessFailed.builder()
                    .errorCode(notFoundException.getMessage())
                    .bankName("BakirBank")
                    .amount(transferRequest.getAmount())
                    .customerId(transferRequest.getFromId())
                    .transactionType(TransactionType.TRANSFER).build());*/

            //throw new NotFoundException(notFoundException.getMessage());
        }
    }

    @KafkaListener(topics = "finalize-transfer-money", groupId = "transfer-group", containerFactory = "moneyTransferKafkaListenerContainerFactory")
    public void processFinalizeTransferMoney(TransferMoneyRequest request) {
        try {
            GetAccountByIban receiver = accountServiceClient.getAccountByIban(request.getToIBAN());
            GetAccountByIban sender = accountServiceClient.getAccountByIban(request.getFromIBAN());

            if (receiver == null || sender == null) {
                log.info("Account not found -> accountId: " + request.getFromIBAN());
                throw new NotFoundException(ACCOUNT_NOT_FOUND);
            }

            StringBuilder sb = new StringBuilder();
            if (request.getToFirstName() != null && !request.getToFirstName().isEmpty()) {
                sb.append(request.getToFirstName().substring(0, Math.min(3, request.getToFirstName().length())) + "**** ");
            }
            if (request.getToSecondName() != null && !request.getToSecondName().isEmpty()) {
                sb.append(" ").append(request.getToSecondName().substring(0, Math.min(3, request.getToSecondName().length())) + "**** ");
            }
            if (request.getToLastName() != null && !request.getToLastName().isEmpty()) {
                sb.append(" ").append(request.getToLastName().substring(0, Math.min(3, request.getToLastName().length())) + "**** ");
            }
            String receiverFullName = sb.toString().trim();


            String receiverNotificationMessage = String.format(
                    "Sevgili müşteri %s, %s hesabından %.2f tutarındaki transferiniz başarıyla gerçekleşti.",
                    receiver.getFirstName(),
                    receiverFullName,
                    request.getAmount()
            );

            String senderNotificationMessage = String.format(
                    "Sevgili müşteri %s, %s hesabına %.2f tutarındaki transferiniz başarıyla gerçekleşti.",
                    sender.getFirstName(),
                    receiverFullName,
                    request.getAmount()
            );

            notificationKafkaTemplate.send("notification-service", SendNotificationRequest.builder()
                    .type("INFO")
                    .title("Transfer İşlemi Başarılı")
                    .userId(receiver.getUserId())
                    .message(receiverNotificationMessage)
                    .build());

            notificationKafkaTemplate.send("notification-service", SendNotificationRequest.builder()
                    .type("INFO")
                    .title("Transfer İşlemi Başarılı")
                    .userId(sender.getUserId())
                    .message(senderNotificationMessage)
                    .build());

            if (request.getByAi().equals("TRUE")) {
                //chatnotificationservice call.
                chatNotificationKafkaTemplate.send("chat-notification-service", ChatNotificationRequest.builder()
                        .title("Para Transferi Başarıyla Tamamlandı")
                        .type("TEXT")
                        .time(LocalDateTime.now())
                        .arguments(new HashMap<>())
                        .message("Transfer işlemi: " + request.getAmount() + " tutarında " + receiverFullName + " hesabına başarıyla tamamlandı.")
                        .build());
            }

            //TODO: SEND EMAIL
            //TODO: CREATE INVOICE AND CALL INVOICE SERVICE

            //Sender
            transactionRepository.save(Transaction.builder()
                    .accountId(sender.getAccountId())
                    .amount(request.getAmount())
                    .currency(sender.getCurrency())
                    .senderFirstName(sender.getFirstName())
                    .senderSecondName(sender.getSecondName())
                    .senderLastName(sender.getLastName())
                    .receiverFirstName(receiver.getFirstName())
                    .receiverSecondName(receiver.getSecondName())
                    .receiverLastName(receiver.getLastName())
                    .receiverTckn("")
                    .receiverIban(request.getToIBAN())
                    .type(TransactionType.EXPENSE)
                    .channel(TransactionChannel.ONLINE_BANKING)
                    .category(TransactionCategory.TRANSFER)
                    .status(TransactionStatus.COMPLETED)
                    .title("Para Transferi Gönderme")
                    .description(request.getDescription())
                    .date(LocalDateTime.now())
                    .updatedDate(LocalDateTime.now())
                    .merchantName("")
                    .transactionCode("")
                    .isRecurring(false)
                    .aiFinalCategory("")
                    .build());

            //receiver
            transactionRepository.save(Transaction.builder()
                    .accountId(receiver.getAccountId())
                    .amount(request.getAmount())
                    .currency(receiver.getCurrency())
                    .senderFirstName(sender.getFirstName())
                    .senderSecondName(sender.getSecondName())
                    .senderLastName(sender.getLastName())
                    .receiverFirstName(receiver.getFirstName())
                    .receiverSecondName(receiver.getSecondName())
                    .receiverLastName(receiver.getLastName())
                    .receiverTckn("")
                    .receiverIban(request.getToIBAN())
                    .type(TransactionType.INCOME)
                    .channel(TransactionChannel.ONLINE_BANKING)
                    .category(TransactionCategory.TRANSFER)
                    .status(TransactionStatus.COMPLETED)
                    .title("Para Transferi Geldi")
                    .description(request.getDescription())
                    .date(LocalDateTime.now())
                    .updatedDate(LocalDateTime.now())
                    .merchantName("")
                    .transactionCode("")
                    .isRecurring(false)
                    .aiFinalCategory("")
                    .build());

            //INVOICE SERVICE MAY CALL HERE CREATE INVOICE SERVICE
            //SEND THIS MESSAGES TO NOTIFICATION AND EMAIL SERVICE
            //EMAIL SERVICE WILL SEND WITH HIM SHOPPING CART INFORMATION INCLUDES PRODUCTS PHOTOS EMAIL TO CUSTOMERS //Bunu stock serviceden felan da yapabiliriz


        } catch (Exception exception) {
            log.error("Error at transfer finalize :  ", exception.getMessage());

            throw new NotFoundException(exception.getMessage());
            /*dltKafkaTemplate.send("money-process.DLT", MoneyProcessFailed.builder()
                    .errorCode(exception.getMessage())
                    .bankName("BakirBank")
                    .amount(transferRequest.getAmount())
                    .customerId(transferRequest.getFromId())
                    .transactionType(TransactionType.TRANSFER).build());*/
        }
    }
}