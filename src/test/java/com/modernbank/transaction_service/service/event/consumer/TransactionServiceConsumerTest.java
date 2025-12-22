package com.modernbank.transaction_service.service.event.consumer;

import com.modernbank.transaction_service.api.client.AccountServiceClient;
import com.modernbank.transaction_service.api.request.*;
import com.modernbank.transaction_service.api.response.GetAccountByIban;
import com.modernbank.transaction_service.entity.Transaction;
import com.modernbank.transaction_service.exception.NotFoundException;
import com.modernbank.transaction_service.model.TransactionErrorEvent;
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
    private KafkaTemplate<String, TransactionErrorEvent> errorEventKafkaTemplate;

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

    @Captor
    private ArgumentCaptor<TransferMoneyRequest> transferMoneyRequestCaptor;

    private TransferMoneyRequest validTransferRequest;
    private GetAccountByIban senderAccount;
    private GetAccountByIban receiverAccount;
    private Transaction senderTransaction;
    private Transaction receiverTransaction;

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
                eq(validTransferRequest),
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
                eq(validTransferRequest),
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
                eq(validTransferRequest),
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
        receiverAccount.setFirstName("Jane"); // Different from request
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
                eq(validTransferRequest),
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
        verify(accountServiceClient).updateBalance(eq(validTransferRequest.getFromIBAN()), eq(-100.0));
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
    void transactionServiceConsumer_should_throw_NotFoundException_when_fraud_decision_is_block() {
        // Given
        when(accountServiceClient.getAccountByIban(validTransferRequest.getFromIBAN())).thenReturn(senderAccount);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getToIBAN())).thenReturn(receiverAccount);
        when(transactionRepository.existsDuplicateTransaction(
                anyString(), anyString(), anyDouble(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(fraudEvaluationService.isAccountBlocked(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(senderTransaction);
        when(fraudEvaluationService.evaluateAndDecide(any(Transaction.class), anyString())).thenReturn(FraudDecision.BLOCK);

        // When & Then
        assertThrows(NotFoundException.class, () ->
            transactionServiceConsumer.processStartTransferMoney(validTransferRequest)
        );

        verify(fraudEvaluationService).incrementFraudCounter(eq(senderAccount.getAccountId()), anyString());
    }

    @Test
    void transactionServiceConsumer_should_throw_IllegalArgumentException_when_request_is_null() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            transactionServiceConsumer.processStartTransferMoney(null)
        );
    }

    @Test
    void transactionServiceConsumer_should_throw_IllegalArgumentException_when_fromIBAN_is_null() {
        // Given
        validTransferRequest.setFromIBAN(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            transactionServiceConsumer.processStartTransferMoney(validTransferRequest)
        );
    }

    @Test
    void transactionServiceConsumer_should_throw_IllegalArgumentException_when_toIBAN_is_null() {
        // Given
        validTransferRequest.setToIBAN(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            transactionServiceConsumer.processStartTransferMoney(validTransferRequest)
        );
    }

    @Test
    void transactionServiceConsumer_should_throw_IllegalArgumentException_when_amount_is_zero() {
        // Given
        validTransferRequest.setAmount(0);

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            transactionServiceConsumer.processStartTransferMoney(validTransferRequest)
        );
    }

    @Test
    void transactionServiceConsumer_should_throw_IllegalArgumentException_when_amount_is_negative() {
        // Given
        validTransferRequest.setAmount(-100);

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            transactionServiceConsumer.processStartTransferMoney(validTransferRequest)
        );
    }

    @Test
    void transactionServiceConsumer_should_handle_technical_error_when_exception_occurs() {
        // Given
        when(accountServiceClient.getAccountByIban(anyString())).thenThrow(new RuntimeException("Connection error"));

        // When
        transactionServiceConsumer.processStartTransferMoney(validTransferRequest);

        // Then
        verify(technicalErrorService).handleTechnicalError(
                eq(validTransferRequest),
                anyString(),
                any(Exception.class)
        );
    }

    @Test
    void transactionServiceConsumer_should_process_without_fraud_check_when_fraud_detection_disabled() {
        // Given
        ReflectionTestUtils.setField(transactionServiceConsumer, "fraudDetectionEnabled", false);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getFromIBAN())).thenReturn(senderAccount);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getToIBAN())).thenReturn(receiverAccount);
        when(transactionRepository.existsDuplicateTransaction(
                anyString(), anyString(), anyDouble(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(senderTransaction);

        // When
        transactionServiceConsumer.processStartTransferMoney(validTransferRequest);

        // Then
        verify(fraudEvaluationService, never()).evaluateAndDecide(any(), anyString());
        verify(accountServiceClient).updateBalance(eq(validTransferRequest.getFromIBAN()), eq(-100.0));
        verify(transferMoneyKafkaTemplate).send(eq("update-transfer-money"), any(TransferMoneyRequest.class));
    }

    // ==================== processUpdateTransferMoney Tests ====================

    @Test
    void transactionServiceConsumer_should_update_balance_and_send_to_finalize_when_update_transfer_success() {
        // Given
        validTransferRequest.setSenderTransactionId("sender-transaction-id");
        validTransferRequest.setReceiverTransactionId("receiver-transaction-id");
        when(transactionRepository.findById("sender-transaction-id")).thenReturn(Optional.of(senderTransaction));

        // When
        transactionServiceConsumer.processUpdateTransferMoney(validTransferRequest);

        // Then
        verify(accountServiceClient).updateBalance(eq(validTransferRequest.getToIBAN()), eq(100.0));
        verify(transferMoneyKafkaTemplate).send(eq("finalize-transfer-money"), eq(validTransferRequest));
    }

    @Test
    void transactionServiceConsumer_should_rollback_when_update_transfer_fails() {
        // Given
        validTransferRequest.setSenderTransactionId("sender-transaction-id");
        validTransferRequest.setReceiverTransactionId("receiver-transaction-id");
        doThrow(new RuntimeException("Update failed")).when(accountServiceClient).updateBalance(eq(validTransferRequest.getToIBAN()), anyDouble());
        when(transactionRepository.findById("receiver-transaction-id")).thenReturn(Optional.of(receiverTransaction));

        // When
        transactionServiceConsumer.processUpdateTransferMoney(validTransferRequest);

        // Then
        verify(accountServiceClient).updateBalance(eq(validTransferRequest.getFromIBAN()), eq(100.0)); // Rollback
        verify(technicalErrorService).handleTechnicalError(eq(validTransferRequest), anyString(), any(Exception.class));
    }

    // ==================== processFinalizeTransferMoney Tests ====================
    @Test
    void transactionServiceConsumer_should_complete_transfer_when_finalize_success() {
        // Given
        validTransferRequest.setSenderTransactionId("sender-transaction-id");
        validTransferRequest.setReceiverTransactionId("receiver-transaction-id");
        validTransferRequest.setByAi("FALSE");
        when(accountServiceClient.getAccountByIban(validTransferRequest.getToIBAN())).thenReturn(receiverAccount);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getFromIBAN())).thenReturn(senderAccount);
        when(transactionRepository.findById("sender-transaction-id")).thenReturn(Optional.of(senderTransaction));
        when(transactionRepository.findById("receiver-transaction-id")).thenReturn(Optional.of(receiverTransaction));

        // When
        transactionServiceConsumer.processFinalizeTransferMoney(validTransferRequest);

        // Then
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        verify(notificationKafkaTemplate, times(2)).send(eq("notification-service"), any(SendNotificationRequest.class));
        verify(dynamicInvoiceKafkaTemplate).send(eq("send-invoice-service"), any(DynamicInvoiceRequest.class));
    }

    @Test
    void transactionServiceConsumer_should_skip_when_finalize_request_is_null() {
        // When
        transactionServiceConsumer.processFinalizeTransferMoney(null);

        // Then
        verify(accountServiceClient, never()).getAccountByIban(anyString());
        verify(transactionRepository, never()).findById(anyString());
    }

    @Test
    void transactionServiceConsumer_should_skip_when_sender_transaction_id_is_null() {
        // Given
        validTransferRequest.setSenderTransactionId(null);
        validTransferRequest.setReceiverTransactionId("receiver-transaction-id");

        // When
        transactionServiceConsumer.processFinalizeTransferMoney(validTransferRequest);

        // Then
        verify(accountServiceClient, never()).getAccountByIban(anyString());
    }

    @Test
    void transactionServiceConsumer_should_skip_when_receiver_transaction_id_is_null() {
        // Given
        validTransferRequest.setSenderTransactionId("sender-transaction-id");
        validTransferRequest.setReceiverTransactionId(null);

        // When
        transactionServiceConsumer.processFinalizeTransferMoney(validTransferRequest);

        // Then
        verify(accountServiceClient, never()).getAccountByIban(anyString());
    }

    @Test
    void transactionServiceConsumer_should_throw_NotFoundException_when_sender_transaction_not_found() {
        // Given
        validTransferRequest.setSenderTransactionId("sender-transaction-id");
        validTransferRequest.setReceiverTransactionId("receiver-transaction-id");
        validTransferRequest.setByAi("FALSE");
        when(accountServiceClient.getAccountByIban(validTransferRequest.getToIBAN())).thenReturn(receiverAccount);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getFromIBAN())).thenReturn(senderAccount);
        when(transactionRepository.findById("sender-transaction-id")).thenReturn(Optional.empty());

        // When
        transactionServiceConsumer.processFinalizeTransferMoney(validTransferRequest);

        // Then
        verify(technicalErrorService).handleTechnicalError(eq(validTransferRequest), anyString(), any(Exception.class));
    }

    @Test
    void transactionServiceConsumer_should_throw_NotFoundException_when_receiver_transaction_not_found() {
        // Given
        validTransferRequest.setSenderTransactionId("sender-transaction-id");
        validTransferRequest.setReceiverTransactionId("receiver-transaction-id");
        validTransferRequest.setByAi("FALSE");
        when(accountServiceClient.getAccountByIban(validTransferRequest.getToIBAN())).thenReturn(receiverAccount);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getFromIBAN())).thenReturn(senderAccount);
        when(transactionRepository.findById("sender-transaction-id")).thenReturn(Optional.of(senderTransaction));
        when(transactionRepository.findById("receiver-transaction-id")).thenReturn(Optional.empty());

        // When
        transactionServiceConsumer.processFinalizeTransferMoney(validTransferRequest);

        // Then
        verify(technicalErrorService).handleTechnicalError(eq(validTransferRequest), anyString(), any(Exception.class));
    }

    @Test
    void transactionServiceConsumer_should_send_chat_notification_when_byAi_is_true() {
        // Given
        validTransferRequest.setSenderTransactionId("sender-transaction-id");
        validTransferRequest.setReceiverTransactionId("receiver-transaction-id");
        validTransferRequest.setByAi("TRUE");
        when(accountServiceClient.getAccountByIban(validTransferRequest.getToIBAN())).thenReturn(receiverAccount);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getFromIBAN())).thenReturn(senderAccount);
        when(transactionRepository.findById("sender-transaction-id")).thenReturn(Optional.of(senderTransaction));
        when(transactionRepository.findById("receiver-transaction-id")).thenReturn(Optional.of(receiverTransaction));

        // When
        transactionServiceConsumer.processFinalizeTransferMoney(validTransferRequest);

        // Then
        verify(chatNotificationKafkaTemplate).send(eq("chat-notification-service"), any(ChatNotificationRequest.class));
    }

    @Test
    void transactionServiceConsumer_should_not_send_chat_notification_when_byAi_is_false() {
        // Given
        validTransferRequest.setSenderTransactionId("sender-transaction-id");
        validTransferRequest.setReceiverTransactionId("receiver-transaction-id");
        validTransferRequest.setByAi("FALSE");
        when(accountServiceClient.getAccountByIban(validTransferRequest.getToIBAN())).thenReturn(receiverAccount);
        when(accountServiceClient.getAccountByIban(validTransferRequest.getFromIBAN())).thenReturn(senderAccount);
        when(transactionRepository.findById("sender-transaction-id")).thenReturn(Optional.of(senderTransaction));
        when(transactionRepository.findById("receiver-transaction-id")).thenReturn(Optional.of(receiverTransaction));

        // When
        transactionServiceConsumer.processFinalizeTransferMoney(validTransferRequest);

        // Then
        verify(chatNotificationKafkaTemplate, never()).send(anyString(), any(ChatNotificationRequest.class));
    }

    @Test
    void transactionServiceConsumer_should_handle_technical_error_when_finalize_fails() {
        validTransferRequest.setSenderTransactionId("sender-transaction-id");
        validTransferRequest.setReceiverTransactionId("receiver-transaction-id");
        when(accountServiceClient.getAccountByIban(anyString())).thenThrow(new RuntimeException("Connection error"));

        transactionServiceConsumer.processFinalizeTransferMoney(validTransferRequest);

        verify(technicalErrorService).handleTechnicalError(eq(validTransferRequest), anyString(), any(Exception.class));
    }

    @Test
    void transactionServiceConsumer_should_log_when_consume_withdraw_money() {
        WithdrawAndDepositMoneyRequest request = new WithdrawAndDepositMoneyRequest();

        assertDoesNotThrow(() -> transactionServiceConsumer.consumeWithdrawMoney(request));
    }

    @Test
    void transactionServiceConsumer_should_log_when_consume_deposit_money() {
        WithdrawAndDepositMoneyRequest request = new WithdrawAndDepositMoneyRequest();

        assertDoesNotThrow(() -> transactionServiceConsumer.consumeDepositMoney(request));
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

        verify(technicalErrorService, never()).handleBusinessError(
                any(), any(), any(), contains("RECEIVER"));
    }

    @Test
    void transactionServiceConsumer_should_process_when_toFirstName_is_null() {
        // Given
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

        verify(technicalErrorService, never()).handleBusinessError(
                any(), any(), any(), contains("RECEIVER"));
    }
}

