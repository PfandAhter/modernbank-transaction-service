package com.modernbank.transaction_service.config;

import com.modernbank.transaction_service.api.request.SendNotificationRequest;
import com.modernbank.transaction_service.rest.controller.request.TransferMoneyRequest;
import com.modernbank.transaction_service.rest.controller.request.WithdrawAndDepositMoneyRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConfiguration {

    //MONEY WITHDRAW AND DEPOSIT KAFKA

    @Bean
    public ProducerFactory<String, WithdrawAndDepositMoneyRequest> moneyWithdrawAndDepositProducerFactory(){
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, WithdrawAndDepositMoneyRequest> moneyWithdrawAndDepositKafkaTemplate(){
//        KafkaTemplate<String, WithdrawAndDepositMoneyRequest> kafkaTemplate = new KafkaTemplate<>(moneyWithdrawAndDepositProducerFactory());
        return new KafkaTemplate<>(moneyWithdrawAndDepositProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, WithdrawAndDepositMoneyRequest> moneyWithdrawAndDepositConsumerFactory(){
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "withdraw-and-deposit");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.modernbank.transaction_service.rest.controller.request.WithdrawAndDepositMoneyRequest");

        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(), new JsonDeserializer<>(WithdrawAndDepositMoneyRequest.class));
    }


    //MONEY TRANSFER KAFKA

    @Bean
    public ProducerFactory<String, TransferMoneyRequest> moneyTransferProducerFactory(){
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, TransferMoneyRequest> moneyTransferKafkaTemplate(){
//        KafkaTemplate<String, WithdrawAndDepositMoneyRequest> kafkaTemplate = new KafkaTemplate<>(moneyWithdrawAndDepositProducerFactory());
        return new KafkaTemplate<>(moneyTransferProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, TransferMoneyRequest> moneyTransferConsumerFactory(){
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "transfer-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.modernbank.transaction_service.rest.controller.request.TransferMoneyRequest");
        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(), new JsonDeserializer<>(TransferMoneyRequest.class));
    }

    //Notification service kafka

    @Bean
    public ProducerFactory<String, SendNotificationRequest> notificationServiceProducerFactory(){
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, SendNotificationRequest> notificationServiceKafkaTemplate(){
//        KafkaTemplate<String, WithdrawAndDepositMoneyRequest> kafkaTemplate = new KafkaTemplate<>(moneyWithdrawAndDepositProducerFactory());
        return new KafkaTemplate<>(notificationServiceProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, SendNotificationRequest> notificationServiceConsumerFactory(){
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.modernbank.transaction_service.api.request.SendNotificationRequest");

        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(), new JsonDeserializer<>(SendNotificationRequest.class));
    }
}