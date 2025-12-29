package com.modernbank.transaction_service.service.event.consumer;

import com.modernbank.transaction_service.api.client.AccountServiceClient;
import com.modernbank.transaction_service.api.dto.AccountDTO;
import com.modernbank.transaction_service.api.request.*;
import com.modernbank.transaction_service.api.response.GetAccountByIban;
import com.modernbank.transaction_service.api.response.GetAccountByIdResponse;
import com.modernbank.transaction_service.entity.Transaction;
import com.modernbank.transaction_service.model.enums.*;
import com.modernbank.transaction_service.repository.TransactionRepository;
import com.modernbank.transaction_service.service.FraudEvaluationService;
import com.modernbank.transaction_service.service.TechnicalErrorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceConsumerTest {

    @Mock
    private KafkaTemplate<String, TransferMoneyRequest> transferMoneyKafkaTemplate;

    @Mock
    private KafkaTemplate<String, SendNotificationRequest> notificationKafkaTemplate;

    @Mock
    private KafkaTemplate<String, ChatNotificationRequest> chatNotificationKafkaTemplate;

    @Mock
    private KafkaTemplate<String, DynamicInvoiceRequest> dynamicInvoiceKafkaTemplate;

    @Mock
    private TechnicalErrorService technicalErrorService;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private FraudEvaluationService fraudEvaluationService;

    @InjectMocks
    private TransactionServiceConsumer transactionServiceConsumer;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    private TransferMoneyRequest validTransferRequest;
    private GetAccountByIban senderAccount;
    private GetAccountByIban receiverAccount;
    private Transaction senderTransaction;
    private Transaction receiverTransaction;
    private WithdrawAndDepositMoneyRequest withdrawDepositRequest;
    private GetAccountByIdResponse accountByIdResponse;
    private AccountDTO accountDTO;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(transactionServiceConsumer, "fraudDetectionEnabled", true);

        validTransferRequest = TransferMoneyRequest.builder()
                .fromIBAN("TR123456789012345678901234")
                .toIBAN("TR987654321098765432109876")
                .amount(100.0)
                .description("Test transfer")
                .toFirstName("John")
                .toSecondName("")
                .toLastName("Doe")
                .byAi("FALSE")
                .build();
        validTransferRequest.setUserId("user123");

        senderAccount = new GetAccountByIban();
        senderAccount.setAccountId("sender-account-id");
        senderAccount.setUserId("sender-user-id");
        senderAccount.setFirstName("Alice");
        senderAccount.setSecondName("");
        senderAccount.setLastName("Smith");
        senderAccount.setBalance(1000.0);
        senderAccount.setCurrency(Currency.TRY);
        senderAccount.setTckn("12345678901");
        senderAccount.setAccountName("Sender Account");

        receiverAccount = new GetAccountByIban();
        receiverAccount.setAccountId("receiver-account-id");
        receiverAccount.setUserId("receiver-user-id");
        receiverAccount.setFirstName("John");
        receiverAccount.setSecondName("");
        receiverAccount.setLastName("Doe");
        receiverAccount.setBalance(500.0);
        receiverAccount.setCurrency(Currency.TRY);
        receiverAccount.setTckn("98765432109");
        receiverAccount.setAccountName("Receiver Account");

        senderTransaction = Transaction.builder()
                .id("sender-transaction-id")
                .accountId("sender-account-id")
                .amount(100.0)
                .status(TransactionStatus.INITIATED)
                .currency(Currency.TRY)
                .type(TransactionType.EXPENSE)
                .channel(TransactionChannel.ONLINE_BANKING)
                .category(TransactionCategory.TRANSFER)
                .date(LocalDateTime.now())
                .build();

        receiverTransaction = Transaction.builder()
                .id("receiver-transaction-id")
                .accountId("receiver-account-id")
                .amount(100.0)
                .status(TransactionStatus.INITIATED)
                .currency(Currency.TRY)
                .type(TransactionType.INCOME)
                .channel(TransactionChannel.ONLINE_BANKING)
                .category(TransactionCategory.TRANSFER)
                .date(LocalDateTime.now())
                .build();

        // Setup for withdraw/deposit tests
        withdrawDepositRequest = new WithdrawAndDepositMoneyRequest();
        withdrawDepositRequest.setAccountId("test-account-id");
        withdrawDepositRequest.setAmount(200.0);
        withdrawDepositRequest.setUserId("test-user-id");

        accountDTO = new AccountDTO();
        accountDTO.setId("test-account-id");
        accountDTO.setUserId("test-user-id");
        accountDTO.setFirstName("Test");
        accountDTO.setSecondName("");
        accountDTO.setLastName("User");
        accountDTO.setBalance(1000.0);
        accountDTO.setCurrency(Currency.TRY);
        accountDTO.setIban("TR111111111111111111111111");

        accountByIdResponse = new GetAccountByIdResponse(accountDTO);
    }

    // ==================== processStartTransferMoney Tests ====================

    @Test
    void transactionServiceConsumer_should_skip_when_duplicate_transaction_detected() {
        // Given
        when(accountServiceClient.getAccountByIban(validTransferRequest.getFromIBAN())).thenReturn(senderAccount);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getToIBAN())).thenReturn(receiverAccount);
        when(transactionRepository.existsDuplicateTransaction(
                anyString(), anyString(), anyDouble(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

        // When
        transactionServiceConsumer.processStartTransferMoney(validTransferRequest);

        // Then
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(transferMoneyKafkaTemplate, never()).send(anyString(), any(TransferMoneyRequest.class));
    }

    @Test
    void transactionServiceConsumer_should_handle_business_error_when_insufficient_funds() {
        senderAccount.setBalance(50.0);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getFromIBAN())).thenReturn(senderAccount);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getToIBAN())).thenReturn(receiverAccount);
        when(transactionRepository.existsDuplicateTransaction(
                anyString(), anyString(), anyDouble(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

        transactionServiceConsumer.processStartTransferMoney(validTransferRequest);

        verify(technicalErrorService).handleBusinessError(
                isNull(),
                eq(validTransferRequest.getUserId()),
                anyString(),
                eq(50.0),
                eq(100.0)
        );
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transactionServiceConsumer_should_handle_business_error_when_account_blocked() {
        // Given
        when(accountServiceClient.getAccountByIban(validTransferRequest.getFromIBAN())).thenReturn(senderAccount);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getToIBAN())).thenReturn(receiverAccount);
        when(transactionRepository.existsDuplicateTransaction(
                anyString(), anyString(), anyDouble(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(fraudEvaluationService.isAccountBlocked(senderAccount.getAccountId())).thenReturn(true);

        // When
        transactionServiceConsumer.processStartTransferMoney(validTransferRequest);

        // Then
        verify(technicalErrorService).handleBusinessError(
                isNull(),
                eq(validTransferRequest.getUserId()),
                anyString()
        );
    }

    @Test
    void transactionServiceConsumer_should_handle_business_error_when_currency_mismatch() {
        // Given
        receiverAccount.setCurrency(Currency.USD);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getFromIBAN())).thenReturn(senderAccount);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getToIBAN())).thenReturn(receiverAccount);
        when(transactionRepository.existsDuplicateTransaction(
                anyString(), anyString(), anyDouble(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(fraudEvaluationService.isAccountBlocked(anyString())).thenReturn(false);

        // When
        transactionServiceConsumer.processStartTransferMoney(validTransferRequest);

        // Then
        verify(technicalErrorService).handleBusinessError(
                isNull(),
                eq(validTransferRequest.getUserId()),
                anyString(),
                eq(Currency.TRY),
                eq(Currency.USD)
        );
    }

    @Test
    void transactionServiceConsumer_should_handle_business_error_when_receiver_name_mismatch() {
        // Given
        receiverAccount.setFirstName("Jane");
        when(accountServiceClient.getAccountByIban(validTransferRequest.getFromIBAN())).thenReturn(senderAccount);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getToIBAN())).thenReturn(receiverAccount);
        when(transactionRepository.existsDuplicateTransaction(
                anyString(), anyString(), anyDouble(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(fraudEvaluationService.isAccountBlocked(anyString())).thenReturn(false);

        // When
        transactionServiceConsumer.processStartTransferMoney(validTransferRequest);

        // Then
        verify(technicalErrorService).handleBusinessError(
                isNull(),
                eq(validTransferRequest.getUserId()),
                anyString()
        );
    }

    @Test
    void transactionServiceConsumer_should_approve_transfer_when_fraud_decision_is_approve() {
        // Given
        when(accountServiceClient.getAccountByIban(validTransferRequest.getFromIBAN())).thenReturn(senderAccount);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getToIBAN())).thenReturn(receiverAccount);
        when(transactionRepository.existsDuplicateTransaction(
                anyString(), anyString(), anyDouble(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(fraudEvaluationService.isAccountBlocked(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(senderTransaction).thenReturn(receiverTransaction);
        when(fraudEvaluationService.evaluateAndDecide(any(Transaction.class), anyString())).thenReturn(FraudDecision.APPROVE);

        // When
        transactionServiceConsumer.processStartTransferMoney(validTransferRequest);

        // Then
        verify(accountServiceClient).updateBalance(validTransferRequest.getFromIBAN(), -100.0);
        verify(transferMoneyKafkaTemplate).send(eq("update-transfer-money"), any(TransferMoneyRequest.class));
    }

    @Test
    void transactionServiceConsumer_should_hold_transfer_when_fraud_decision_is_hold() {
        // Given
        when(accountServiceClient.getAccountByIban(validTransferRequest.getFromIBAN())).thenReturn(senderAccount);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getToIBAN())).thenReturn(receiverAccount);
        when(transactionRepository.existsDuplicateTransaction(
                anyString(), anyString(), anyDouble(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(fraudEvaluationService.isAccountBlocked(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(senderTransaction);
        when(fraudEvaluationService.evaluateAndDecide(any(Transaction.class), anyString())).thenReturn(FraudDecision.HOLD);

        // When
        transactionServiceConsumer.processStartTransferMoney(validTransferRequest);

        // Then
        verify(transactionRepository, atLeastOnce()).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(TransactionStatus.HOLD, savedTransaction.getStatus());
        verify(notificationKafkaTemplate).send(eq("notification-service"), any(SendNotificationRequest.class));
    }

    @Test
    void transactionServiceConsumer_should_block_transfer_when_fraud_decision_is_block() {
        // Given
        when(accountServiceClient.getAccountByIban(validTransferRequest.getFromIBAN())).thenReturn(senderAccount);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getToIBAN())).thenReturn(receiverAccount);
        when(transactionRepository.existsDuplicateTransaction(
                anyString(), anyString(), anyDouble(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(fraudEvaluationService.isAccountBlocked(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(senderTransaction);
        when(fraudEvaluationService.evaluateAndDecide(any(Transaction.class), anyString())).thenReturn(FraudDecision.BLOCK);

        // When
        transactionServiceConsumer.processStartTransferMoney(validTransferRequest);

        // Then
        verify(fraudEvaluationService).incrementFraudCounter(eq(senderAccount.getAccountId()), anyString());
        verify(transactionRepository, atLeastOnce()).save(transactionCaptor.capture());
        assertEquals(TransactionStatus.BLOCKED, transactionCaptor.getValue().getStatus());
    }

    @Test
    void transactionServiceConsumer_should_throw_IllegalArgumentException_when_request_is_null() {
        assertThrows(IllegalArgumentException.class, () ->
            transactionServiceConsumer.processStartTransferMoney(null)
        );
    }

    @Test
    void transactionServiceConsumer_should_throw_IllegalArgumentException_when_fromIBAN_is_null() {
        validTransferRequest.setFromIBAN(null);
        assertThrows(IllegalArgumentException.class, () ->
            transactionServiceConsumer.processStartTransferMoney(validTransferRequest)
        );
    }

    @Test
    void transactionServiceConsumer_should_throw_IllegalArgumentException_when_toIBAN_is_null() {
        validTransferRequest.setToIBAN(null);
        assertThrows(IllegalArgumentException.class, () ->
            transactionServiceConsumer.processStartTransferMoney(validTransferRequest)
        );
    }

    @Test
    void transactionServiceConsumer_should_throw_IllegalArgumentException_when_amount_is_zero() {
        validTransferRequest.setAmount(0);
        assertThrows(IllegalArgumentException.class, () ->
            transactionServiceConsumer.processStartTransferMoney(validTransferRequest)
        );
    }

    @Test
    void transactionServiceConsumer_should_throw_IllegalArgumentException_when_amount_is_negative() {
        validTransferRequest.setAmount(-100);
        assertThrows(IllegalArgumentException.class, () ->
            transactionServiceConsumer.processStartTransferMoney(validTransferRequest)
        );
    }

    @Test
    void transactionServiceConsumer_should_handle_technical_error_when_exception_occurs() {
        when(accountServiceClient.getAccountByIban(anyString())).thenThrow(new RuntimeException("Connection error"));

        transactionServiceConsumer.processStartTransferMoney(validTransferRequest);

        verify(technicalErrorService).handleTechnicalError(anyString(), any(Exception.class));
    }

    @Test
    void transactionServiceConsumer_should_process_without_fraud_check_when_fraud_detection_disabled() {
        ReflectionTestUtils.setField(transactionServiceConsumer, "fraudDetectionEnabled", false);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getFromIBAN())).thenReturn(senderAccount);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getToIBAN())).thenReturn(receiverAccount);
        when(transactionRepository.existsDuplicateTransaction(
                anyString(), anyString(), anyDouble(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(senderTransaction);

        transactionServiceConsumer.processStartTransferMoney(validTransferRequest);

        verify(fraudEvaluationService, never()).evaluateAndDecide(any(), anyString());
        verify(accountServiceClient).updateBalance(validTransferRequest.getFromIBAN(), -100.0);
        verify(transferMoneyKafkaTemplate).send(eq("update-transfer-money"), any(TransferMoneyRequest.class));
    }

    // ==================== processUpdateTransferMoney Tests ====================

    @Test
    void transactionServiceConsumer_should_update_balance_and_send_to_finalize_when_update_transfer_success() {
        validTransferRequest.setSenderTransactionId("sender-transaction-id");
        validTransferRequest.setReceiverTransactionId("receiver-transaction-id");
        when(transactionRepository.findById("sender-transaction-id")).thenReturn(Optional.of(senderTransaction));

        transactionServiceConsumer.processUpdateTransferMoney(validTransferRequest);

        verify(accountServiceClient).updateBalance(validTransferRequest.getToIBAN(), 100.0);
        verify(transferMoneyKafkaTemplate).send("finalize-transfer-money", validTransferRequest);
    }

    @Test
    void transactionServiceConsumer_should_rollback_when_update_transfer_fails() {
        validTransferRequest.setSenderTransactionId("sender-transaction-id");
        validTransferRequest.setReceiverTransactionId("receiver-transaction-id");
        doThrow(new RuntimeException("Update failed")).when(accountServiceClient).updateBalance(eq(validTransferRequest.getToIBAN()), anyDouble());
        when(transactionRepository.findById("receiver-transaction-id")).thenReturn(Optional.of(receiverTransaction));

        transactionServiceConsumer.processUpdateTransferMoney(validTransferRequest);

        verify(accountServiceClient).updateBalance(validTransferRequest.getFromIBAN(), 100.0);
        verify(technicalErrorService).handleTechnicalError(anyString(), any(Exception.class));
    }

    // ==================== processFinalizeTransferMoney Tests ====================

    @Test
    void transactionServiceConsumer_should_complete_transfer_when_finalize_success() {
        validTransferRequest.setSenderTransactionId("sender-transaction-id");
        validTransferRequest.setReceiverTransactionId("receiver-transaction-id");
        validTransferRequest.setByAi("FALSE");
        when(accountServiceClient.getAccountByIban(validTransferRequest.getToIBAN())).thenReturn(receiverAccount);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getFromIBAN())).thenReturn(senderAccount);
        when(transactionRepository.findById("sender-transaction-id")).thenReturn(Optional.of(senderTransaction));
        when(transactionRepository.findById("receiver-transaction-id")).thenReturn(Optional.of(receiverTransaction));

        transactionServiceConsumer.processFinalizeTransferMoney(validTransferRequest);

        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        verify(dynamicInvoiceKafkaTemplate).send(eq("send-invoice-service"), any(DynamicInvoiceRequest.class));
    }

    @Test
    void transactionServiceConsumer_should_skip_when_finalize_request_is_null() {
        transactionServiceConsumer.processFinalizeTransferMoney(null);

        verify(accountServiceClient, never()).getAccountByIban(anyString());
        verify(transactionRepository, never()).findById(anyString());
    }

    @Test
    void transactionServiceConsumer_should_skip_when_sender_transaction_id_is_null() {
        validTransferRequest.setSenderTransactionId(null);
        validTransferRequest.setReceiverTransactionId("receiver-transaction-id");

        transactionServiceConsumer.processFinalizeTransferMoney(validTransferRequest);

        verify(accountServiceClient, never()).getAccountByIban(anyString());
    }

    @Test
    void transactionServiceConsumer_should_skip_when_receiver_transaction_id_is_null() {
        validTransferRequest.setSenderTransactionId("sender-transaction-id");
        validTransferRequest.setReceiverTransactionId(null);

        transactionServiceConsumer.processFinalizeTransferMoney(validTransferRequest);

        verify(accountServiceClient, never()).getAccountByIban(anyString());
    }

    @Test
    void transactionServiceConsumer_should_handle_error_when_sender_transaction_not_found() {
        validTransferRequest.setSenderTransactionId("sender-transaction-id");
        validTransferRequest.setReceiverTransactionId("receiver-transaction-id");
        validTransferRequest.setByAi("FALSE");
        when(accountServiceClient.getAccountByIban(validTransferRequest.getToIBAN())).thenReturn(receiverAccount);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getFromIBAN())).thenReturn(senderAccount);
        when(transactionRepository.findById("sender-transaction-id")).thenReturn(Optional.empty());

        transactionServiceConsumer.processFinalizeTransferMoney(validTransferRequest);

        verify(technicalErrorService).handleTechnicalError(anyString(), any(Exception.class));
    }

    @Test
    void transactionServiceConsumer_should_handle_error_when_receiver_transaction_not_found() {
        validTransferRequest.setSenderTransactionId("sender-transaction-id");
        validTransferRequest.setReceiverTransactionId("receiver-transaction-id");
        validTransferRequest.setByAi("FALSE");
        when(accountServiceClient.getAccountByIban(validTransferRequest.getToIBAN())).thenReturn(receiverAccount);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getFromIBAN())).thenReturn(senderAccount);
        when(transactionRepository.findById("sender-transaction-id")).thenReturn(Optional.of(senderTransaction));
        when(transactionRepository.findById("receiver-transaction-id")).thenReturn(Optional.empty());

        transactionServiceConsumer.processFinalizeTransferMoney(validTransferRequest);

        verify(technicalErrorService).handleTechnicalError(anyString(), any(Exception.class));
    }

    @Test
    void transactionServiceConsumer_should_send_chat_notification_when_byAi_is_true() {
        validTransferRequest.setSenderTransactionId("sender-transaction-id");
        validTransferRequest.setReceiverTransactionId("receiver-transaction-id");
        validTransferRequest.setByAi("TRUE");
        when(accountServiceClient.getAccountByIban(validTransferRequest.getToIBAN())).thenReturn(receiverAccount);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getFromIBAN())).thenReturn(senderAccount);
        when(transactionRepository.findById("sender-transaction-id")).thenReturn(Optional.of(senderTransaction));
        when(transactionRepository.findById("receiver-transaction-id")).thenReturn(Optional.of(receiverTransaction));

        transactionServiceConsumer.processFinalizeTransferMoney(validTransferRequest);

        verify(chatNotificationKafkaTemplate).send(eq("chat-notification-service"), any(ChatNotificationRequest.class));
    }

    @Test
    void transactionServiceConsumer_should_not_send_chat_notification_when_byAi_is_false() {
        validTransferRequest.setSenderTransactionId("sender-transaction-id");
        validTransferRequest.setReceiverTransactionId("receiver-transaction-id");
        validTransferRequest.setByAi("FALSE");
        when(accountServiceClient.getAccountByIban(validTransferRequest.getToIBAN())).thenReturn(receiverAccount);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getFromIBAN())).thenReturn(senderAccount);
        when(transactionRepository.findById("sender-transaction-id")).thenReturn(Optional.of(senderTransaction));
        when(transactionRepository.findById("receiver-transaction-id")).thenReturn(Optional.of(receiverTransaction));

        transactionServiceConsumer.processFinalizeTransferMoney(validTransferRequest);

        verify(chatNotificationKafkaTemplate, never()).send(anyString(), any(ChatNotificationRequest.class));
    }

    @Test
    void transactionServiceConsumer_should_handle_technical_error_when_finalize_fails() {
        validTransferRequest.setSenderTransactionId("sender-transaction-id");
        validTransferRequest.setReceiverTransactionId("receiver-transaction-id");
        when(accountServiceClient.getAccountByIban(anyString())).thenThrow(new RuntimeException("Connection error"));

        transactionServiceConsumer.processFinalizeTransferMoney(validTransferRequest);

        verify(technicalErrorService).handleTechnicalError(anyString(), any(Exception.class));
    }

    @Test
    void transactionServiceConsumer_should_process_when_toFirstName_is_empty() {
        validTransferRequest.setToFirstName("");
        when(accountServiceClient.getAccountByIban(validTransferRequest.getFromIBAN())).thenReturn(senderAccount);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getToIBAN())).thenReturn(receiverAccount);
        when(transactionRepository.existsDuplicateTransaction(
                anyString(), anyString(), anyDouble(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(fraudEvaluationService.isAccountBlocked(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(senderTransaction).thenReturn(receiverTransaction);
        when(fraudEvaluationService.evaluateAndDecide(any(Transaction.class), anyString())).thenReturn(FraudDecision.APPROVE);

        transactionServiceConsumer.processStartTransferMoney(validTransferRequest);

        verify(technicalErrorService, never()).handleBusinessError(any(), any(), contains("RECEIVER"));
    }

    @Test
    void transactionServiceConsumer_should_process_when_toFirstName_is_null() {
        validTransferRequest.setToFirstName(null);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getFromIBAN())).thenReturn(senderAccount);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getToIBAN())).thenReturn(receiverAccount);
        when(transactionRepository.existsDuplicateTransaction(
                anyString(), anyString(), anyDouble(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(fraudEvaluationService.isAccountBlocked(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(senderTransaction).thenReturn(receiverTransaction);
        when(fraudEvaluationService.evaluateAndDecide(any(Transaction.class), anyString())).thenReturn(FraudDecision.APPROVE);

        transactionServiceConsumer.processStartTransferMoney(validTransferRequest);

        verify(technicalErrorService, never()).handleBusinessError(any(), any(), contains("RECEIVER"));
    }

    // ==================== consumeWithdrawMoney Tests ====================

    @Test
    void transactionServiceConsumer_should_complete_withdraw_when_approved() {
        when(accountServiceClient.getAccountById(withdrawDepositRequest.getAccountId())).thenReturn(accountByIdResponse);
        when(transactionRepository.existsDuplicateWithdrawDeposit(
                anyString(), anyDouble(), any(TransactionType.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(fraudEvaluationService.isAccountBlocked(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId("withdraw-transaction-id");
            return t;
        });
        when(fraudEvaluationService.evaluateAndDecide(any(Transaction.class), anyString())).thenReturn(FraudDecision.APPROVE);

        transactionServiceConsumer.consumeWithdrawMoney(withdrawDepositRequest);

        verify(accountServiceClient).updateBalance(accountDTO.getIban(), -200.0);
        verify(transactionRepository, atLeastOnce()).save(transactionCaptor.capture());
        assertEquals(TransactionStatus.COMPLETED, transactionCaptor.getValue().getStatus());
    }

    @Test
    void transactionServiceConsumer_should_skip_withdraw_when_duplicate_detected() {
        when(accountServiceClient.getAccountById(withdrawDepositRequest.getAccountId())).thenReturn(accountByIdResponse);
        when(transactionRepository.existsDuplicateWithdrawDeposit(
                anyString(), anyDouble(), any(TransactionType.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

        transactionServiceConsumer.consumeWithdrawMoney(withdrawDepositRequest);

        verify(accountServiceClient, never()).updateBalance(anyString(), anyDouble());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transactionServiceConsumer_should_return_business_error_when_insufficient_funds_for_withdraw() {
        accountDTO.setBalance(100.0);
        when(accountServiceClient.getAccountById(withdrawDepositRequest.getAccountId())).thenReturn(accountByIdResponse);
        when(transactionRepository.existsDuplicateWithdrawDeposit(
                anyString(), anyDouble(), any(TransactionType.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

        transactionServiceConsumer.consumeWithdrawMoney(withdrawDepositRequest);

        verify(technicalErrorService).handleBusinessError(
                isNull(),
                eq(withdrawDepositRequest.getUserId()),
                anyString(),
                eq(100.0),
                eq(200.0)
        );
    }

    @Test
    void transactionServiceConsumer_should_return_business_error_when_account_blocked_for_withdraw() {
        when(accountServiceClient.getAccountById(withdrawDepositRequest.getAccountId())).thenReturn(accountByIdResponse);
        when(transactionRepository.existsDuplicateWithdrawDeposit(
                anyString(), anyDouble(), any(TransactionType.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(fraudEvaluationService.isAccountBlocked(accountDTO.getId())).thenReturn(true);

        transactionServiceConsumer.consumeWithdrawMoney(withdrawDepositRequest);

        verify(technicalErrorService).handleBusinessError(
                isNull(),
                eq(withdrawDepositRequest.getUserId()),
                anyString()
        );
    }

    @Test
    void transactionServiceConsumer_should_hold_withdraw_when_fraud_decision_is_hold() {
        when(accountServiceClient.getAccountById(withdrawDepositRequest.getAccountId())).thenReturn(accountByIdResponse);
        when(transactionRepository.existsDuplicateWithdrawDeposit(
                anyString(), anyDouble(), any(TransactionType.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(fraudEvaluationService.isAccountBlocked(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId("withdraw-transaction-id");
            return t;
        });
        when(fraudEvaluationService.evaluateAndDecide(any(Transaction.class), anyString())).thenReturn(FraudDecision.HOLD);

        transactionServiceConsumer.consumeWithdrawMoney(withdrawDepositRequest);

        verify(transactionRepository, atLeastOnce()).save(transactionCaptor.capture());
        assertEquals(TransactionStatus.HOLD, transactionCaptor.getValue().getStatus());
        verify(notificationKafkaTemplate).send(eq("notification-service"), any(SendNotificationRequest.class));
    }

    @Test
    void transactionServiceConsumer_should_block_withdraw_when_fraud_decision_is_block() {
        when(accountServiceClient.getAccountById(withdrawDepositRequest.getAccountId())).thenReturn(accountByIdResponse);
        when(transactionRepository.existsDuplicateWithdrawDeposit(
                anyString(), anyDouble(), any(TransactionType.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(fraudEvaluationService.isAccountBlocked(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId("withdraw-transaction-id");
            return t;
        });
        when(fraudEvaluationService.evaluateAndDecide(any(Transaction.class), anyString())).thenReturn(FraudDecision.BLOCK);

        transactionServiceConsumer.consumeWithdrawMoney(withdrawDepositRequest);

        verify(fraudEvaluationService).incrementFraudCounter(anyString(), anyString());
        verify(transactionRepository, atLeastOnce()).save(transactionCaptor.capture());
        assertEquals(TransactionStatus.BLOCKED, transactionCaptor.getValue().getStatus());
    }

    @Test
    void transactionServiceConsumer_should_process_withdraw_without_fraud_check_when_disabled() {
        ReflectionTestUtils.setField(transactionServiceConsumer, "fraudDetectionEnabled", false);
        when(accountServiceClient.getAccountById(withdrawDepositRequest.getAccountId())).thenReturn(accountByIdResponse);
        when(transactionRepository.existsDuplicateWithdrawDeposit(
                anyString(), anyDouble(), any(TransactionType.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId("withdraw-transaction-id");
            return t;
        });

        transactionServiceConsumer.consumeWithdrawMoney(withdrawDepositRequest);

        verify(fraudEvaluationService, never()).evaluateAndDecide(any(), anyString());
        verify(accountServiceClient).updateBalance(accountDTO.getIban(), -200.0);
    }

    @Test
    void transactionServiceConsumer_should_handle_technical_error_when_withdraw_fails() {
        when(accountServiceClient.getAccountById(anyString())).thenThrow(new RuntimeException("Connection error"));

        transactionServiceConsumer.consumeWithdrawMoney(withdrawDepositRequest);

        verify(technicalErrorService).handleTechnicalError(anyString(), any(Exception.class));
    }

    // ==================== consumeDepositMoney Tests ====================

    @Test
    void transactionServiceConsumer_should_complete_deposit_when_approved() {
        when(accountServiceClient.getAccountById(withdrawDepositRequest.getAccountId())).thenReturn(accountByIdResponse);
        when(transactionRepository.existsDuplicateWithdrawDeposit(
                anyString(), anyDouble(), any(TransactionType.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(fraudEvaluationService.isAccountBlocked(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId("deposit-transaction-id");
            return t;
        });
        when(fraudEvaluationService.evaluateAndDecide(any(Transaction.class), anyString())).thenReturn(FraudDecision.APPROVE);

        transactionServiceConsumer.consumeDepositMoney(withdrawDepositRequest);

        verify(accountServiceClient).updateBalance(accountDTO.getIban(), 200.0);
        verify(transactionRepository, atLeastOnce()).save(transactionCaptor.capture());
        assertEquals(TransactionStatus.COMPLETED, transactionCaptor.getValue().getStatus());
    }

    @Test
    void transactionServiceConsumer_should_skip_deposit_when_duplicate_detected() {
        when(accountServiceClient.getAccountById(withdrawDepositRequest.getAccountId())).thenReturn(accountByIdResponse);
        when(transactionRepository.existsDuplicateWithdrawDeposit(
                anyString(), anyDouble(), any(TransactionType.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

        transactionServiceConsumer.consumeDepositMoney(withdrawDepositRequest);

        verify(accountServiceClient, never()).updateBalance(anyString(), anyDouble());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transactionServiceConsumer_should_return_business_error_when_account_blocked_for_deposit() {
        when(accountServiceClient.getAccountById(withdrawDepositRequest.getAccountId())).thenReturn(accountByIdResponse);
        when(transactionRepository.existsDuplicateWithdrawDeposit(
                anyString(), anyDouble(), any(TransactionType.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(fraudEvaluationService.isAccountBlocked(accountDTO.getId())).thenReturn(true);

        transactionServiceConsumer.consumeDepositMoney(withdrawDepositRequest);

        verify(technicalErrorService).handleBusinessError(
                isNull(),
                eq(withdrawDepositRequest.getUserId()),
                anyString()
        );
    }

    @Test
    void transactionServiceConsumer_should_hold_deposit_when_fraud_decision_is_hold() {
        when(accountServiceClient.getAccountById(withdrawDepositRequest.getAccountId())).thenReturn(accountByIdResponse);
        when(transactionRepository.existsDuplicateWithdrawDeposit(
                anyString(), anyDouble(), any(TransactionType.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(fraudEvaluationService.isAccountBlocked(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId("deposit-transaction-id");
            return t;
        });
        when(fraudEvaluationService.evaluateAndDecide(any(Transaction.class), anyString())).thenReturn(FraudDecision.HOLD);

        transactionServiceConsumer.consumeDepositMoney(withdrawDepositRequest);

        verify(transactionRepository, atLeastOnce()).save(transactionCaptor.capture());
        assertEquals(TransactionStatus.HOLD, transactionCaptor.getValue().getStatus());
        verify(notificationKafkaTemplate).send(eq("notification-service"), any(SendNotificationRequest.class));
    }

    @Test
    void transactionServiceConsumer_should_block_deposit_when_fraud_decision_is_block() {
        when(accountServiceClient.getAccountById(withdrawDepositRequest.getAccountId())).thenReturn(accountByIdResponse);
        when(transactionRepository.existsDuplicateWithdrawDeposit(
                anyString(), anyDouble(), any(TransactionType.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(fraudEvaluationService.isAccountBlocked(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId("deposit-transaction-id");
            return t;
        });
        when(fraudEvaluationService.evaluateAndDecide(any(Transaction.class), anyString())).thenReturn(FraudDecision.BLOCK);

        transactionServiceConsumer.consumeDepositMoney(withdrawDepositRequest);

        verify(fraudEvaluationService).incrementFraudCounter(anyString(), anyString());
        verify(transactionRepository, atLeastOnce()).save(transactionCaptor.capture());
        assertEquals(TransactionStatus.BLOCKED, transactionCaptor.getValue().getStatus());
    }

    @Test
    void transactionServiceConsumer_should_process_deposit_without_fraud_check_when_disabled() {
        ReflectionTestUtils.setField(transactionServiceConsumer, "fraudDetectionEnabled", false);
        when(accountServiceClient.getAccountById(withdrawDepositRequest.getAccountId())).thenReturn(accountByIdResponse);
        when(transactionRepository.existsDuplicateWithdrawDeposit(
                anyString(), anyDouble(), any(TransactionType.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId("deposit-transaction-id");
            return t;
        });

        transactionServiceConsumer.consumeDepositMoney(withdrawDepositRequest);

        verify(fraudEvaluationService, never()).evaluateAndDecide(any(), anyString());
        verify(accountServiceClient).updateBalance(accountDTO.getIban(), 200.0);
    }

    @Test
    void transactionServiceConsumer_should_handle_technical_error_when_deposit_fails() {
        when(accountServiceClient.getAccountById(anyString())).thenThrow(new RuntimeException("Connection error"));

        transactionServiceConsumer.consumeDepositMoney(withdrawDepositRequest);

        verify(technicalErrorService).handleTechnicalError(anyString(), any(Exception.class));
    }

    @Test
    void transactionServiceConsumer_should_create_transaction_with_correct_type_for_withdraw() {
        when(accountServiceClient.getAccountById(withdrawDepositRequest.getAccountId())).thenReturn(accountByIdResponse);
        when(transactionRepository.existsDuplicateWithdrawDeposit(
                anyString(), anyDouble(), any(TransactionType.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(fraudEvaluationService.isAccountBlocked(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fraudEvaluationService.evaluateAndDecide(any(Transaction.class), anyString())).thenReturn(FraudDecision.APPROVE);

        transactionServiceConsumer.consumeWithdrawMoney(withdrawDepositRequest);

        verify(transactionRepository, atLeastOnce()).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getAllValues().get(0);
        assertEquals(TransactionType.EXPENSE, savedTransaction.getType());
        assertEquals(TransactionCategory.WITHDRAWAL, savedTransaction.getCategory());
    }

    @Test
    void transactionServiceConsumer_should_create_transaction_with_correct_type_for_deposit() {
        when(accountServiceClient.getAccountById(withdrawDepositRequest.getAccountId())).thenReturn(accountByIdResponse);
        when(transactionRepository.existsDuplicateWithdrawDeposit(
                anyString(), anyDouble(), any(TransactionType.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(fraudEvaluationService.isAccountBlocked(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fraudEvaluationService.evaluateAndDecide(any(Transaction.class), anyString())).thenReturn(FraudDecision.APPROVE);

        transactionServiceConsumer.consumeDepositMoney(withdrawDepositRequest);

        verify(transactionRepository, atLeastOnce()).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getAllValues().get(0);
        assertEquals(TransactionType.INCOME, savedTransaction.getType());
        assertEquals(TransactionCategory.DEPOSIT, savedTransaction.getCategory());
    }
}

