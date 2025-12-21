package com.modernbank.transaction_service.config;

import com.modernbank.transaction_service.api.request.*;
import com.modernbank.transaction_service.model.TransactionErrorEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConfiguration {

    // MONEY WITHDRAW AND DEPOSIT KAFKA

    @Bean
    public ProducerFactory<String, WithdrawAndDepositMoneyRequest> moneyWithdrawAndDepositProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, WithdrawAndDepositMoneyRequest> moneyWithdrawAndDepositKafkaTemplate() {
        // KafkaTemplate<String, WithdrawAndDepositMoneyRequest> kafkaTemplate = new
        // KafkaTemplate<>(moneyWithdrawAndDepositProducerFactory());
        return new KafkaTemplate<>(moneyWithdrawAndDepositProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, WithdrawAndDepositMoneyRequest> moneyWithdrawAndDepositConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "withdraw-and-deposit");
        configProps.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, JsonDeserializer.class);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.modernbank.transaction_service.api.request");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                "com.modernbank.transaction_service.api.request.WithdrawAndDepositMoneyRequest");

        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(),
                new JsonDeserializer<>(WithdrawAndDepositMoneyRequest.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, WithdrawAndDepositMoneyRequest> moneyWithdrawAndDepositKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, WithdrawAndDepositMoneyRequest> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(moneyWithdrawAndDepositConsumerFactory());
        // DefaultErrorHandler ile ilişkilendir
        factory.setCommonErrorHandler(defaultErrorHandler());
        return factory;
    }

    // MONEY TRANSFER KAFKA

    @Bean
    public ProducerFactory<String, TransferMoneyRequest> moneyTransferProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, TransferMoneyRequest> moneyTransferKafkaTemplate() {
        // KafkaTemplate<String, WithdrawAndDepositMoneyRequest> kafkaTemplate = new
        // KafkaTemplate<>(moneyWithdrawAndDepositProducerFactory());
        return new KafkaTemplate<>(moneyTransferProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, TransferMoneyRequest> moneyTransferConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "transfer-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TransferMoneyRequest.class.getName());
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransferMoneyRequest> moneyTransferKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TransferMoneyRequest> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(moneyTransferConsumerFactory());
        // DefaultErrorHandler ile ilişkilendir
        factory.setCommonErrorHandler(testTransferMoneyRequestErrorHandler());
        return factory;
    }


    @Bean
    public DefaultErrorHandler testTransferMoneyRequestErrorHandler() {
        FixedBackOff fixedBackOff = new FixedBackOff(2000L, 3);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(fixedBackOff);

        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                NullPointerException.class,
                org.springframework.kafka.support.serializer.DeserializationException.class,
                com.modernbank.transaction_service.exception.NotFoundException.class
        );

        return errorHandler;
    }

    // Notification service kafka

    @Bean
    public ProducerFactory<String, SendNotificationRequest> notificationServiceProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, SendNotificationRequest> notificationServiceKafkaTemplate() {
        // KafkaTemplate<String, WithdrawAndDepositMoneyRequest> kafkaTemplate = new
        // KafkaTemplate<>(moneyWithdrawAndDepositProducerFactory());
        return new KafkaTemplate<>(notificationServiceProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, SendNotificationRequest> notificationServiceConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                "com.modernbank.transaction_service.api.request.SendNotificationRequest");

        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(),
                new JsonDeserializer<>(SendNotificationRequest.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SendNotificationRequest> notificationKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, SendNotificationRequest> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(notificationServiceConsumerFactory());
        // DefaultErrorHandler ile ilişkilendir
        factory.setCommonErrorHandler(defaultErrorHandler());
        return factory;
    }

    // Withdraw From ATM Kafka

    @Bean
    public ProducerFactory<String, WithdrawFromATMRequest> withdrawFromATMServiceProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, WithdrawFromATMRequest> withdrawFromATMServiceKafkaTemplate() {
        // KafkaTemplate<String, WithdrawAndDepositMoneyRequest> kafkaTemplate = new
        // KafkaTemplate<>(moneyWithdrawAndDepositProducerFactory());
        return new KafkaTemplate<>(withdrawFromATMServiceProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, WithdrawFromATMRequest> withdrawFromATMServiceConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "withdraw-money-from-atm-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.modernbank.transaction_service.api.request");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                "com.modernbank.transaction_service.api.request.WithdrawFromATMRequest");
        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(),
                new JsonDeserializer<>(WithdrawFromATMRequest.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, WithdrawFromATMRequest> withdrawFromATMKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, WithdrawFromATMRequest> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(withdrawFromATMServiceConsumerFactory());
        // DefaultErrorHandler ile ilişkilendir
        factory.setCommonErrorHandler(defaultErrorHandler());
        return factory;
    }

    // Transfer money to atm Kafka

    @Bean
    public ProducerFactory<String, TransferMoneyATMRequest> transferMoneyToATMServiceProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, TransferMoneyATMRequest> transferMoneyToATMServiceKafkaTemplate() {
        // KafkaTemplate<String, WithdrawAndDepositMoneyRequest> kafkaTemplate = new
        // KafkaTemplate<>(moneyWithdrawAndDepositProducerFactory());
        return new KafkaTemplate<>(transferMoneyToATMServiceProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, TransferMoneyATMRequest> transferMoneyToATMServiceConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "transfer-money-to-atm-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, JsonDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.modernbank.transaction_service.api.request");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                "com.modernbank.transaction_service.api.request.TransferMoneyATMRequest");
        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(),
                new JsonDeserializer<>(TransferMoneyATMRequest.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransferMoneyATMRequest> transferMoneyToATMKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TransferMoneyATMRequest> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(transferMoneyToATMServiceConsumerFactory());
        // DefaultErrorHandler ile ilişkilendir
        factory.setCommonErrorHandler(defaultErrorHandler());
        return factory;
    }

    // Send Chat Notification KAFKA
    @Bean
    public ProducerFactory<String, ChatNotificationRequest> sendChatNotificationKafkaProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, ChatNotificationRequest> sendChatNotificationKafkaTemplate() {
        // KafkaTemplate<String, WithdrawAndDepositMoneyRequest> kafkaTemplate = new
        // KafkaTemplate<>(moneyWithdrawAndDepositProducerFactory());
        return new KafkaTemplate<>(sendChatNotificationKafkaProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, ChatNotificationRequest> sendChatNotificationKafkaConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "send-chat-notification-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, JsonDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                "com.modernbank.transaction_service.api.request.ChatNotificationRequest");
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(),
                new JsonDeserializer<>(ChatNotificationRequest.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ChatNotificationRequest> sendChatNotificationKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ChatNotificationRequest> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(sendChatNotificationKafkaConsumerFactory());
        // DefaultErrorHandler ile ilişkilendir
        factory.setCommonErrorHandler(defaultErrorHandler());
        return factory;
    }

    // SEND GENERATE INVOICE KAFKA
    @Bean
    public ProducerFactory<String, DynamicInvoiceRequest> sendGenerateInvoiceKafkaProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, DynamicInvoiceRequest> sendGenerateInvoiceKafkaTemplate() {
        // KafkaTemplate<String, WithdrawAndDepositMoneyRequest> kafkaTemplate = new
        // KafkaTemplate<>(moneyWithdrawAndDepositProducerFactory());
        return new KafkaTemplate<>(sendGenerateInvoiceKafkaProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, DynamicInvoiceRequest> sendGenerateInvoiceKafkaConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "send-invoice-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, JsonDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                "com.modernbank.transaction_service.api.request.DynamicInvoiceRequest");
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(),
                new JsonDeserializer<>(DynamicInvoiceRequest.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DynamicInvoiceRequest> sendGenerateInvoiceKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DynamicInvoiceRequest> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(sendGenerateInvoiceKafkaConsumerFactory());
        // DefaultErrorHandler ile ilişkilendir
        factory.setCommonErrorHandler(defaultErrorHandler());
        return factory;
    }


    // ==================== FRAUD DECISION KAFKA ====================



    // ==================== TRANSACTION RISK EVALUATED EVENT KAFKA
    // ====================


    @Bean
    public DefaultErrorHandler defaultErrorHandler() {
        FixedBackOff fixedBackOff = new FixedBackOff(3000L, 0);

        return new DefaultErrorHandler(fixedBackOff);
    }

    // ==================== ERROR HANDLER TOPIC ====================

    @Bean
    public ProducerFactory<String, TransactionErrorEvent> errorEventProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, TransactionErrorEvent> errorEventKafkaTemplate() {
        return new KafkaTemplate<>(errorEventProducerFactory());
    }
}