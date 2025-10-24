package com.modernbank.transaction_service.service.event.producer;


import com.modernbank.transaction_service.api.request.TransferMoneyRequest;
import com.modernbank.transaction_service.api.request.WithdrawAndDepositMoneyRequest;
import com.modernbank.transaction_service.api.response.BaseResponse;
import com.modernbank.transaction_service.service.event.ITransactionServiceProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j

public class TransactionServiceProducerImpl implements ITransactionServiceProducer {

    private final KafkaTemplate<String, WithdrawAndDepositMoneyRequest> withdrawAndDepositMoneyKafkaTemplate;

    private final KafkaTemplate<String, TransferMoneyRequest> transferMoneyKafkaTemplate;

    @Override
    public BaseResponse withdrawMoney(WithdrawAndDepositMoneyRequest request) {
        log.info("Sending withdraw money request to Kafka topic");
        withdrawAndDepositMoneyKafkaTemplate.send("${kafka.topics.withdraw-money}", request);
        return new BaseResponse("Withdraw money request sent successfully");
    }

    @Override
    public BaseResponse depositMoney(WithdrawAndDepositMoneyRequest request) {
        log.info("Sending deposit money request to Kafka topic");
        withdrawAndDepositMoneyKafkaTemplate.send("${kafka.topics.deposit-money}", request);
        return new BaseResponse("Deposit money request sent successfully");
    }

    @Override
    public BaseResponse transferMoney(TransferMoneyRequest request) {
        log.info("Sending transfer money request to Kafka topic");
        transferMoneyKafkaTemplate.send("start-transfer-money", request);
        return new BaseResponse("Transfer money request sent successfully");
    }
}