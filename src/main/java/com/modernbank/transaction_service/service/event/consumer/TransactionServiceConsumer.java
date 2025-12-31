package com.modernbank.transaction_service.service.event.consumer;

import com.modernbank.transaction_service.api.client.AccountServiceClient;
import com.modernbank.transaction_service.api.request.*;
import com.modernbank.transaction_service.api.response.GetAccountByIban;
import com.modernbank.transaction_service.api.response.GetAccountByIdResponse;
import com.modernbank.transaction_service.entity.Transaction;
import com.modernbank.transaction_service.exception.NotFoundException;
import com.modernbank.transaction_service.model.enums.*;
import com.modernbank.transaction_service.repository.TransactionRepository;
import com.modernbank.transaction_service.service.FraudEvaluationService;
import com.modernbank.transaction_service.service.TechnicalErrorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.modernbank.transaction_service.constant.ErrorCodeConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j

public class TransactionServiceConsumer {

    private final KafkaTemplate<String, TransferMoneyRequest> transferMoneyKafkaTemplate;

    private final KafkaTemplate<String, SendNotificationRequest> notificationKafkaTemplate;

    private final KafkaTemplate<String, ChatNotificationRequest> chatNotificationKafkaTemplate;

    private final KafkaTemplate<String, DynamicInvoiceRequest> dynamicInvoiceKafkaTemplate;

    private final TechnicalErrorService technicalErrorService;

    private final AccountServiceClient accountServiceClient;

    private final TransactionRepository transactionRepository;

    private final FraudEvaluationService fraudEvaluationService;

    @Value("${fraud.enabled:true}")
    private boolean fraudDetectionEnabled;

    @Transactional
    @KafkaListener(topics = "withdraw-money", groupId = "withdraw-and-deposit", containerFactory = "moneyWithdrawAndDepositKafkaListenerContainerFactory")
    public void consumeWithdrawMoney(WithdrawAndDepositMoneyRequest request) {
        log.info("Received withdraw money request: {}", request);

        try {
            GetAccountByIdResponse account = accountServiceClient.getAccountById(request.getAccountId());

            boolean isDuplicate = transactionRepository.existsDuplicateWithdrawDeposit(
                    request.getAccountId(),
                    request.getAmount(),
                    TransactionType.EXPENSE,
                    LocalDateTime.now().minusMinutes(1),
                    LocalDateTime.now()
            );

            if (isDuplicate) {
                log.warn("Duplicate withdraw message detected! Skipping. AccountId: {}", account.getAccount().getId());
                return;
            }

            if (account.getAccount().getBalance() < request.getAmount()) {
                technicalErrorService.handleBusinessError(
                        null,
                        request.getUserId(),
                        DYNAMIC_INSUFFICIENT_FUNDS,
                        account.getAccount().getBalance(),
                        request.getAmount()
                );
                return;
            }

            if (fraudDetectionEnabled && fraudEvaluationService.isAccountBlocked(account.getAccount().getId())) {
                technicalErrorService.handleBusinessError(
                        null,
                        request.getUserId(),
                        DYNAMIC_ACCOUNT_BLOCKED
                );
                return;
            }

            Transaction transaction = Transaction.builder()
                    .accountId(account.getAccount().getId())
                    .amount(request.getAmount())
                    .currency(account.getAccount().getCurrency())
                    .senderFirstName(account.getAccount().getFirstName())
                    .senderSecondName(account.getAccount().getSecondName())
                    .senderLastName(account.getAccount().getLastName())
                    .type(TransactionType.EXPENSE)
                    .channel(TransactionChannel.DIRECT_WITHDRAWAL)
                    .category(TransactionCategory.WITHDRAWAL)
                    .status(TransactionStatus.INITIATED)
                    .description("İnternet Bankacılığı üzerinden para çekme işlemi")
                    .title("Para Çekme")
                    .date(LocalDateTime.now())
                    .updatedDate(LocalDateTime.now())
                    .isRecurring(false)
                    .build();
            transaction = transactionRepository.save(transaction);

            if (fraudDetectionEnabled) {
                FraudDecision decision = fraudEvaluationService.evaluateAndDecide(transaction, account.getAccount().getId());
                handleWithdrawFraudDecision(decision, transaction, request, account);
            } else {
                processApprovedWithdraw(transaction, request, account);
            }

        } catch (Exception exception) {
            log.error("Error processing withdraw request: {}", exception.getMessage());
            technicalErrorService.handleTechnicalError(
                    //request,
                    TECH_WITHDRAW_MONEY_ERROR,
                    exception
            );
        }
    }

    private void handleWithdrawFraudDecision(
            FraudDecision decision,
            Transaction transaction,
            WithdrawAndDepositMoneyRequest request,
            GetAccountByIdResponse account) {

        switch (decision) {
            case APPROVE -> {
                log.info("Withdraw fraud decision APPROVE: proceeding");
                processApprovedWithdraw(transaction, request, account);
            }
            case HOLD -> {
                log.info("Withdraw fraud decision HOLD: awaiting confirmation");
                processHoldWithdraw(transaction, account);
            }
            case BLOCK -> {
                log.warn("Withdraw fraud decision BLOCK: rejecting");
                processBlockedWithdraw(transaction, account);
            }
        }
    }

    private void processApprovedWithdraw(Transaction transaction, WithdrawAndDepositMoneyRequest request, GetAccountByIdResponse account) {
        accountServiceClient.updateBalance(account.getAccount().getIban(), -request.getAmount());

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setUpdatedDate(LocalDateTime.now());
        transactionRepository.save(transaction);

        String traceId = MDC.get("traceId");
        String message = String.format("Hesabınızdan %.2f %s tutarında para çekme işlemi gerçekleştirildi.",
                request.getAmount(), account.getAccount().getCurrency());
        sendSafeNotification(request.getUserId(), message, "INFO", "Para Çekme İşlemi", traceId);
        updateTransactionLimit(account.getAccount().getId(), request.getAmount(), TransactionCategory.WITHDRAWAL.toString());

        log.info("Withdraw completed: transactionId={}, amount={}", transaction.getId(), request.getAmount());
    }

    private void processHoldWithdraw(Transaction transaction, GetAccountByIdResponse account) {
        transaction.setStatus(TransactionStatus.HOLD);
        transactionRepository.save(transaction);

        String message = String.format("%.2f tutarındaki para çekme işleminiz güvenlik nedeniyle beklemeye alındı.",
                transaction.getAmount());

        HashMap<String, Object> args = new HashMap<>();
        args.put("transactionId", transaction.getId());

        notificationKafkaTemplate.send("notification-service", SendNotificationRequest.builder()
                .userId(account.getAccount().getUserId())
                .title("İşlem Onayı Gerekli")
                .message(message)
                .type("CONFIRMATION_REQUIRED")
                .arguments(args)
                .build());

        log.info("Withdraw held for confirmation: transactionId={}", transaction.getId());
    }

    private void processBlockedWithdraw(Transaction transaction, GetAccountByIdResponse account) {
        transaction.setStatus(TransactionStatus.BLOCKED);
        transactionRepository.save(transaction);

        fraudEvaluationService.incrementFraudCounter(account.getAccount().getUserId(), "HIGH risk withdraw rejected");

        String traceId = MDC.get("traceId");
        sendSafeNotification(account.getAccount().getUserId(), "Güvenlik nedeniyle para çekme işleminiz reddedildi.", "CRITICAL", "İşlem Reddedildi", traceId);

        log.warn("Withdraw blocked: transactionId={}, userId={}", transaction.getId(), account.getAccount().getUserId());
    }


    @Transactional
    @KafkaListener(topics = "deposit-money", groupId = "withdraw-and-deposit", containerFactory = "moneyWithdrawAndDepositKafkaListenerContainerFactory")
    public void consumeDepositMoney(WithdrawAndDepositMoneyRequest request) {
        log.info("Received deposit money request: {}", request);

        try {
            GetAccountByIdResponse account = accountServiceClient.getAccountById(request.getAccountId());

            boolean isDuplicate = transactionRepository.existsDuplicateWithdrawDeposit(
                    request.getAccountId(),
                    request.getAmount(),
                    TransactionType.INCOME,
                    LocalDateTime.now().minusMinutes(1),
                    LocalDateTime.now()
            );

            if (isDuplicate) {
                log.warn("Duplicate deposit message detected! Skipping. AccountId: {}", account.getAccount().getId());
                return;
            }

            if (fraudDetectionEnabled && fraudEvaluationService.isAccountBlocked(account.getAccount().getId())) {
                technicalErrorService.handleBusinessError(
                        null,
                        request.getUserId(),
                        DYNAMIC_ACCOUNT_BLOCKED
                );
                return;
            }

            Transaction transaction = Transaction.builder()
                    .accountId(account.getAccount().getId())
                    .amount(request.getAmount())
                    .currency(account.getAccount().getCurrency())
                    .senderFirstName(account.getAccount().getFirstName())
                    .senderSecondName(account.getAccount().getSecondName())
                    .senderLastName(account.getAccount().getLastName())
                    .type(TransactionType.INCOME)
                    .channel(TransactionChannel.DIRECT_DEPOSIT)
                    .category(TransactionCategory.DEPOSIT)
                    .status(TransactionStatus.INITIATED)
                    .description("İnternet Bankacılığı üzerinden para yatırma işlemi")
                    .title("Para Yatırma")
                    .date(LocalDateTime.now())
                    .updatedDate(LocalDateTime.now())
                    .isRecurring(false)
                    .build();
            transaction = transactionRepository.save(transaction);

            if (fraudDetectionEnabled) {
                FraudDecision decision = fraudEvaluationService.evaluateAndDecide(transaction, account.getAccount().getId());
                handleDepositFraudDecision(decision, transaction, request, account);
            } else {
                processApprovedDeposit(transaction, request, account);
            }

        } catch (Exception exception) {
            log.error("Error processing deposit request: {}", exception.getMessage());
            technicalErrorService.handleTechnicalError(
                    TECH_DEPOSIT_MONEY_ERROR,
                    exception
            );
        }
    }

    private void handleDepositFraudDecision(
            FraudDecision decision,
            Transaction transaction,
            WithdrawAndDepositMoneyRequest request,
            GetAccountByIdResponse account) {

        switch (decision) {
            case APPROVE -> {
                log.info("Deposit fraud decision APPROVE: proceeding");
                processApprovedDeposit(transaction, request, account);
            }
            case HOLD -> {
                log.info("Deposit fraud decision HOLD: awaiting confirmation");
                processHoldDeposit(transaction, account);
            }
            case BLOCK -> {
                log.warn("Deposit fraud decision BLOCK: rejecting");
                processBlockedDeposit(transaction, account);
            }
        }
    }

    private void processApprovedDeposit(Transaction transaction, WithdrawAndDepositMoneyRequest request, GetAccountByIdResponse account) {
        accountServiceClient.updateBalance(account.getAccount().getIban(), request.getAmount());

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setUpdatedDate(LocalDateTime.now());
        transactionRepository.save(transaction);

        String traceId = MDC.get("traceId");
        String message = String.format("Hesabınıza %.2f %s tutarında para yatırma işlemi gerçekleştirildi.",
                request.getAmount(), account.getAccount().getCurrency());
        sendSafeNotification(request.getUserId(), message, "INFO", "Para Yatırma İşlemi", traceId);
        updateTransactionLimit(account.getAccount().getId(), request.getAmount(), TransactionCategory.DEPOSIT.toString());

        log.info("Deposit completed: transactionId={}, amount={}", transaction.getId(), request.getAmount());
    }

    private void processHoldDeposit(Transaction transaction, GetAccountByIdResponse account) {
        transaction.setStatus(TransactionStatus.HOLD);
        transactionRepository.save(transaction);

        String message = String.format("%.2f tutarındaki para yatırma işleminiz güvenlik nedeniyle beklemeye alındı.",
                transaction.getAmount());

        HashMap<String, Object> args = new HashMap<>();
        args.put("transactionId", transaction.getId());

        notificationKafkaTemplate.send("notification-service", SendNotificationRequest.builder()
                .userId(account.getAccount().getUserId())
                .title("İşlem Onayı Gerekli")
                .message(message)
                .type("CONFIRMATION_REQUIRED")
                .arguments(args)
                .build());

        log.info("Deposit held for confirmation: transactionId={}", transaction.getId());
    }

    private void processBlockedDeposit(Transaction transaction, GetAccountByIdResponse account) {
        transaction.setStatus(TransactionStatus.BLOCKED);
        transactionRepository.save(transaction);

        fraudEvaluationService.incrementFraudCounter(account.getAccount().getUserId(), "HIGH risk deposit rejected");

        String traceId = MDC.get("traceId");
        sendSafeNotification(account.getAccount().getUserId(), "Güvenlik nedeniyle para yatırma işleminiz reddedildi.", "CRITICAL", "İşlem Reddedildi", traceId);

        log.warn("Deposit blocked: transactionId={}, userId={}", transaction.getId(), account.getAccount().getUserId());
    }

    @Transactional
    @KafkaListener(topics = "start-transfer-money", groupId = "transfer-group", containerFactory = "moneyTransferKafkaListenerContainerFactory")
    public void processStartTransferMoney(TransferMoneyRequest request) {
        log.info("Received transfer money request: {}", request);
        try {
            validateTransferRequest(request);

            GetAccountByIban senderAccountByIban = accountServiceClient.getAccountByIban(request.getFromIBAN());
            GetAccountByIban receiverAccountByIban = accountServiceClient.getAccountByIban(request.getToIBAN());

            boolean isDuplicate = transactionRepository.existsDuplicateTransaction(
                    request.getFromIBAN(),
                    request.getToIBAN(),
                    request.getAmount(),
                    request.getDescription(),
                    LocalDateTime.now().minusMinutes(1),
                    LocalDateTime.now()
            );

            if (isDuplicate) {
                log.warn("Duplicate message detected! Skipping execution. User: {}", request.getUserId()); //TODO: Should we notify user about duplicate?
                return;
            }

            if (senderAccountByIban.getBalance() < request.getAmount()) {
                technicalErrorService.handleBusinessError(
                        null,
                        request.getUserId(),
                        DYNAMIC_INSUFFICIENT_FUNDS,
                        senderAccountByIban.getBalance(), // Argüman {0}
                        request.getAmount()               // Argüman {1}
                );
                return;
            }

            if (fraudDetectionEnabled && fraudEvaluationService.isAccountBlocked(senderAccountByIban.getAccountId())) {
                technicalErrorService.handleBusinessError(
                        null,
                        request.getUserId(),
                        DYNAMIC_ACCOUNT_BLOCKED
                );
                return;
            }

            if (!senderAccountByIban.getCurrency().equals(receiverAccountByIban.getCurrency())) {
                technicalErrorService.handleBusinessError(
                        null,
                        request.getUserId(),
                        DYNAMIC_CURRENCY_MISMATCH,
                        senderAccountByIban.getCurrency(),   // {0}
                        receiverAccountByIban.getCurrency()  // {1}
                );
                return;
            }

            if (request.getToFirstName() != null && !request.getToFirstName().isEmpty()) {
                if (!receiverAccountByIban.getFirstName().equalsIgnoreCase(request.getToFirstName())) {
                    technicalErrorService.handleBusinessError(
                            null,
                            request.getUserId(),
                            DYNAMIC_RECEIVER_MIS_MATCH
//                            request.getToFirstName()           // {0} Girilen
//                            receiverAccountByIban.getFirstName() // {1} Gerçek
                    );
                    return;
                }
            }

            Transaction transactionRecordForSender = createTransactionRecordForSender(request, senderAccountByIban, receiverAccountByIban);


            request.setSenderTransactionId(transactionRecordForSender.getId());

            if (fraudDetectionEnabled) {
                FraudDecision decision = fraudEvaluationService.evaluateAndDecide(
                        transactionRecordForSender, senderAccountByIban.getAccountId());
                if (decision.equals(FraudDecision.APPROVE)) {
                    Transaction transactionRecordForReceiver = createTransactionRecordForReceiver(request, senderAccountByIban, receiverAccountByIban);
                    request.setReceiverTransactionId(transactionRecordForReceiver.getId());
                }

                handleFraudDecision(decision, transactionRecordForSender, request, senderAccountByIban);
            } else {
                processApprovedTransfer(transactionRecordForSender, request);
            }

        } catch (Exception exception) {
            log.warn("Error at transfer start: {}", exception.getMessage());

            technicalErrorService.handleTechnicalError(
                    //request,
                    TECH_START_TRANSFER_MONEY_ERROR,
                    exception
            );
        }
    }

    private void handleFraudDecision(
            FraudDecision decision,
            Transaction transaction,
            TransferMoneyRequest request,
            GetAccountByIban senderAccount) {
        switch (decision) {
            case APPROVE -> {
                log.info("Fraud decision APPROVE: proceeding with Kafka flow");
                processApprovedTransfer(transaction, request);
            }
            case HOLD -> {
                log.info("Fraud decision HOLD: saving transaction, awaiting confirmation");
                processHoldTransfer(transaction, senderAccount);
            }
            case BLOCK -> {
                log.warn("Fraud decision BLOCK: rejecting transaction");
                processBlockedTransfer(transaction, senderAccount);
            }
        }
    }

    private void processApprovedTransfer(Transaction transaction, TransferMoneyRequest request) {
        accountServiceClient.updateBalance(request.getFromIBAN(), -request.getAmount());

        transaction.setStatus(TransactionStatus.PENDING);
        transactionRepository.save(transaction);

        transferMoneyKafkaTemplate.send("update-transfer-money", request);
        log.info("Transfer approved and sent to Kafka: transactionId={}", transaction.getId());
    }

    private void processHoldTransfer(Transaction transaction, GetAccountByIban senderAccount) {
        transaction.setStatus(TransactionStatus.HOLD);
        transactionRepository.save(transaction);

        String message = transaction.getReceiverIban() + " nolu IBAN'a "+ transaction.getAmount() +" miktarindaki transfer işleminiz güvenlik nedeniyle beklemeye alındı. Lütfen işlemi onaylayınız.";

        HashMap<String, Object> args = new HashMap<>();
        args.put("transactionId", transaction.getId());
        notificationKafkaTemplate.send("notification-service", SendNotificationRequest.builder()
                .userId(senderAccount.getUserId())
                .title("İşlem Onayı Gerekli")
                .message(message)
                .type("CONFIRMATION_REQUIRED")
                .arguments(args)
                .build());

        log.info("Transfer held for confirmation: transactionId={}, userId={}",
                transaction.getId(), senderAccount.getUserId());
    }

    private void processBlockedTransfer(Transaction transaction, GetAccountByIban senderAccount) {
        transaction.setStatus(TransactionStatus.BLOCKED);
        transactionRepository.save(transaction);
        fraudEvaluationService.incrementFraudCounter(
                senderAccount.getAccountId(),
                "HIGH risk transaction rejected");

        String traceId = MDC.get("traceId");
        sendSafeNotification(
                senderAccount.getUserId(),
                "Güvenlik nedeniyle transfer işleminiz reddedildi. Lütfen destek ekibimizle iletişime geçiniz.",
                "CRITICAL",
                "İşlem Reddedildi",
                traceId
        );
        updatePreviousFraudFlagSafe(senderAccount.getAccountId(), true);

        log.warn("Transfer blocked: transactionId={}, userId={}",
                transaction.getId(), senderAccount.getUserId());
        //throw new NotFoundException("Transaction rejected due to security reasons");
    }

    private Transaction createTransactionRecordForSender(
            TransferMoneyRequest request,
            GetAccountByIban senderAccount,
            GetAccountByIban receiverAccount) {
        Transaction transaction = Transaction.builder()
                .accountId(senderAccount.getAccountId())
                .amount(request.getAmount())
                .currency(senderAccount.getCurrency())
                .senderFirstName(senderAccount.getFirstName())
                .senderSecondName(senderAccount.getSecondName())
                .senderLastName(senderAccount.getLastName())
                .receiverTckn("")
                .receiverFirstName(request.getToFirstName())
                .receiverSecondName(request.getToSecondName())
                .receiverLastName(request.getToLastName())
                .receiverIban(request.getToIBAN())
                .type(TransactionType.EXPENSE)
                .channel(TransactionChannel.ONLINE_BANKING)
                .category(TransactionCategory.TRANSFER)
                .status(TransactionStatus.INITIATED)
                .description(request.getDescription())
                .date(LocalDateTime.now())
                .updatedDate(LocalDateTime.now())
                .merchantName("")
                .transactionCode("")
                .isRecurring(false)
                .aiFinalCategory("")
                .build();

        return transactionRepository.save(transaction);
    }

    private Transaction createTransactionRecordForReceiver(
            TransferMoneyRequest request,
            GetAccountByIban sender,
            GetAccountByIban receiver) {
        Transaction transaction = Transaction.builder()
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
                .status(TransactionStatus.INITIATED)
                .description(request.getDescription())
                .date(LocalDateTime.now())
                .updatedDate(LocalDateTime.now())
                .merchantName("")
                .transactionCode("")
                .isRecurring(false)
                .aiFinalCategory("")
                .build();

        return transactionRepository.save(transaction);
    }

    private void validateTransferRequest(TransferMoneyRequest request) {
        java.util.List<String> missingFields = new java.util.ArrayList<>();
        if (request == null) {
            missingFields.add("request");
        } else {
            if (request.getFromIBAN() == null) {
                missingFields.add("fromIBAN");
            }
            if (request.getToIBAN() == null) {
                missingFields.add("toIBAN");
            }
            if (request.getAmount() <= 0) {
                missingFields.add("amount (must be positive)");
            }
        }
        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid transfer request - missing required fields: " + String.join(", ", missingFields));
        }
    }

    @KafkaListener(topics = "update-transfer-money", groupId = "transfer-group", containerFactory = "moneyTransferKafkaListenerContainerFactory")
    public void processUpdateTransferMoney(TransferMoneyRequest request) {
        try {
            accountServiceClient.updateBalance(request.getToIBAN(), request.getAmount());

            transactionRepository.findById(request.getSenderTransactionId())
                    .ifPresent(transaction -> {
                        transaction.setStatus(TransactionStatus.PENDING);
                        transactionRepository.save(transaction);
                    });

            transferMoneyKafkaTemplate.send("finalize-transfer-money", request);
        } catch (Exception exception) {
            log.error("Error at transfer update : ", exception.getMessage());

            accountServiceClient.updateBalance(request.getFromIBAN(), request.getAmount());
            transactionRepository.findById(request.getReceiverTransactionId())
                    .ifPresent(transaction -> {
                        transaction.setStatus(TransactionStatus.FAILED);
                        transactionRepository.save(transaction);
                    });

            technicalErrorService.handleTechnicalError(
                    //request,
                    TECH_UPDATE_TRANSFER_MONEY_ERROR,
                    exception
            );
        }
    }

    @KafkaListener(topics = "finalize-transfer-money", groupId = "transfer-group", containerFactory = "moneyTransferKafkaListenerContainerFactory")
    public void processFinalizeTransferMoney(TransferMoneyRequest request) {
        if (request == null || request.getSenderTransactionId() == null || request.getReceiverTransactionId() == null) {
            log.error("CRITICAL: Received malformed request or missing TransactionID! Dropping message. Request: {}", request);
            return;
        }

        try {
            String traceId = MDC.get("traceId");

            GetAccountByIban receiver = accountServiceClient.getAccountByIban(request.getToIBAN());
            GetAccountByIban sender = accountServiceClient.getAccountByIban(request.getFromIBAN());

            if (receiver == null || sender == null) {
                log.info("Account not found -> accountId: " + request.getFromIBAN());
                throw new NotFoundException(ACCOUNT_NOT_FOUND);
            }

            StringBuilder sb = new StringBuilder();
            if (request.getToFirstName() != null && !request.getToFirstName().isEmpty()) {
                sb.append(request.getToFirstName().substring(0, Math.min(3, request.getToFirstName().length()))
                        + "**** ");
            }
            if (request.getToSecondName() != null && !request.getToSecondName().isEmpty()) {
                sb.append(" ")
                        .append(request.getToSecondName().substring(0, Math.min(3, request.getToSecondName().length()))
                                + "**** ");
            }
            if (request.getToLastName() != null && !request.getToLastName().isEmpty()) {
                sb.append(" ").append(
                        request.getToLastName().substring(0, Math.min(3, request.getToLastName().length())) + "**** ");
            }
            String receiverFullName = sb.toString().trim();

            StringBuilder sbs = new StringBuilder();
            if (sender.getFirstName() != null && !sender.getFirstName().isEmpty()) {
                sbs.append(sender.getFirstName().substring(0, Math.min(3, sender.getFirstName().length())))
                        .append("**** ");
            }
            if (sender.getSecondName() != null && !sender.getSecondName().isEmpty()) {
                sbs.append(sender.getSecondName().substring(0, Math.min(3, sender.getSecondName().length())))
                        .append("**** ");
            }
            if (sender.getLastName() != null && !sender.getLastName().isEmpty()) {
                sbs.append(sender.getLastName().substring(0, Math.min(3, sender.getLastName().length())))
                        .append("**** ");
            }
            String senderFullName = sbs.toString().trim();

            String receiverNotificationMessage = String.format(
                    "Sevgili müşteri %s, %s hesabından %.2f tutarındaki transferiniz başarıyla gerçekleşti.",
                    receiver.getFirstName(),
                    receiverFullName,
                    request.getAmount());

            String senderNotificationMessage = String.format(
                    "Sevgili müşteri %s, %s hesabına %.2f tutarındaki transferiniz başarıyla gerçekleşti.",
                    sender.getFirstName(),
                    receiverFullName,
                    request.getAmount());

            sendSafeNotification(receiver.getUserId(), receiverNotificationMessage,"INFO", "Para Transferi Geldi",traceId);
            sendSafeNotification(sender.getUserId(), senderNotificationMessage, "INFO", "Para Transferi Gönderildi",traceId);

            sendSafeChatNotification(request, sender, receiver, receiverFullName);

            // TODO: SEND EMAIL
            //TODO: ACCOUNTSERVICE ACCOUNT PREVIOUS TRANSACTION IS FRAUD FLAG UPDATE TO FALSE
            updatePreviousFraudFlagSafe(sender.getAccountId(), false);
            updateTransactionLimit(sender.getAccountId(), request.getAmount(), TransactionCategory.TRANSFER.toString());

            // Sender
            Transaction senderTransaction = transactionRepository.findById(request.getSenderTransactionId())
                    .orElseThrow(() -> new NotFoundException("Transaction not found id: " + request.getSenderTransactionId()));

            senderTransaction.setStatus(TransactionStatus.COMPLETED);
            senderTransaction.setTitle("Para Transferi Gönderme");
            senderTransaction.setUpdatedDate(LocalDateTime.now());
            senderTransaction.setInvoiceStatus(InvoiceStatus.PENDING);
            transactionRepository.save(senderTransaction);
            sendSafeCreateInvoice(sender, request, senderFullName, receiverFullName);

            Transaction receiverTransaction = transactionRepository.findById(request.getReceiverTransactionId())
                    .orElseThrow(() -> new NotFoundException("Transaction not found id: " + request.getReceiverTransactionId()));

            receiverTransaction.setStatus(TransactionStatus.COMPLETED);
            receiverTransaction.setTitle("Para Transferi Geldi");
            receiverTransaction.setUpdatedDate(LocalDateTime.now());
            transactionRepository.save(receiverTransaction);

        } catch (Exception exception) {
            log.error("Error at transfer finalize :  ", exception.getMessage());

            technicalErrorService.handleTechnicalError(
                    //request,
                    TECH_FINALIZE_TRANSFER_MONEY_ERROR,
                    exception
            );
        }
    }


    private void sendSafeChatNotification(TransferMoneyRequest request, GetAccountByIban sender, GetAccountByIban receiver, String receiverFullName) {
        try {
            if (request.getByAi().equals("TRUE")) {
                chatNotificationKafkaTemplate.send("chat-notification-service", ChatNotificationRequest.builder()
                        .title("Para Transferi Başarıyla Tamamlandı")
                        .type("notification")
                        .level("success")
                        .userId(sender.getUserId())
                        .time(LocalDateTime.now())
                        .arguments(new HashMap<>())
                        .message("Transfer işlemi: " + request.getAmount() + " tutarında " + receiverFullName
                                + " hesabına başarıyla tamamlandı.")
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to send chat notification for transaction: {}. Error: {}", request.getSenderTransactionId(), e.getMessage());
        }
    }

    private void updatePreviousFraudFlagSafe(String accountId, Boolean flag){
        try{
            accountServiceClient.updatePreviousFraudFlag(accountId, flag);
        }catch(Exception e){
            log.warn("Failed to update previous fraud flag for accountId: {}. Error: {}", accountId, e.getMessage());
        }
    }

    private void updateTransactionLimit(String accountId, Double amount, String category){
        try{
            accountServiceClient.updateLimit(accountId, amount, category);
        }catch(Exception e){
            log.warn("Failed to update transaction limit for accountId: {}. Error: {}", accountId, e.getMessage());
        }
    }

    private void sendSafeNotification(String userId, String message,String type, String title,String traceId) {
        try {
            ProducerRecord<String, SendNotificationRequest> record =
                    new ProducerRecord<>("notification-service", SendNotificationRequest.builder()
                            .type(type)
                            .title(title)
                            .userId(userId)
                            .message(message)
                            .build());
            record.headers().add("traceId", traceId.getBytes());

            notificationKafkaTemplate.send(record);

            /*notificationKafkaTemplate.send("notification-service", SendNotificationRequest.builder()
                    .type(type)
                    .title(title)
                    .userId(notificationSentTo.getUserId())
                    .message(message)
                    .build());*/
        } catch (Exception e) {
            log.warn("Failed to send notification for userId: {}. Error: {}", userId, e.getMessage());
        }
    }

    private void sendSafeCreateInvoice(GetAccountByIban account, TransferMoneyRequest request, String senderFullName, String receiverFullName) {
        try {
            Map<String, Object> invoiceDataForSender = new HashMap<>();
            invoiceDataForSender.put("senderTCKNHashed", account.getTckn());
            invoiceDataForSender.put("senderAccountName", account.getAccountName());
            invoiceDataForSender.put("senderFullName", senderFullName);
            invoiceDataForSender.put("amount", request.getAmount());
            invoiceDataForSender.put("senderAccountIBAN", request.getFromIBAN());
            invoiceDataForSender.put("description", request.getDescription());
            invoiceDataForSender.put("receiverFullName", receiverFullName);
            invoiceDataForSender.put("receiverIBAN", request.getToIBAN());
            invoiceDataForSender.put("currency", account.getCurrency());
            invoiceDataForSender.put("transactionId", request.getSenderTransactionId());
            invoiceDataForSender.put("date", LocalDateTime.now());

            dynamicInvoiceKafkaTemplate.send("send-invoice-service", DynamicInvoiceRequest.builder()
                    .invoiceId("")
                    .userId(account.getUserId())
                    .invoiceType("TRANSFER")
                    .date(LocalDateTime.now())
                    .data(invoiceDataForSender)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to create invoice for transaction: {}. Error: {}", request.getSenderTransactionId(), e.getMessage());
        }
    }
}