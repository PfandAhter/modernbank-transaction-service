package com.modernbank.transaction_service.rest.service.event.producer;


import com.modernbank.transaction_service.rest.controller.request.TransferMoneyRequest;
import com.modernbank.transaction_service.rest.controller.request.WithdrawAndDepositMoneyRequest;
import com.modernbank.transaction_service.rest.controller.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j

public class TransactionServiceProducer {

    private final KafkaTemplate<String, WithdrawAndDepositMoneyRequest> withdrawAndDepositMoneyKafkaTemplate;

    private final KafkaTemplate<String, TransferMoneyRequest> transferMoneyKafkaTemplate;

    public BaseResponse withdrawMoney(WithdrawAndDepositMoneyRequest request) {
        log.info("Sending withdraw money request to Kafka topic");
        withdrawAndDepositMoneyKafkaTemplate.send("${kafka.topics.withdraw-money}", request);
        return new BaseResponse("Withdraw money request sent successfully");
    }

    public BaseResponse depositMoney(WithdrawAndDepositMoneyRequest request) {
        log.info("Sending deposit money request to Kafka topic");
        withdrawAndDepositMoneyKafkaTemplate.send("${kafka.topics.deposit-money}", request);
        return new BaseResponse("Deposit money request sent successfully");
    }

    public BaseResponse transferMoney(TransferMoneyRequest request) {
        log.info("Sending transfer money request to Kafka topic");
        transferMoneyKafkaTemplate.send("${kafka.topics.transfer-money}", request);
        return new BaseResponse("Transfer money request sent successfully");
    }
}