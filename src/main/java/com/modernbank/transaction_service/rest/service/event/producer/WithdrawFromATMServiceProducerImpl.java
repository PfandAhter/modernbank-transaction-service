package com.modernbank.transaction_service.rest.service.event.producer;

import com.modernbank.transaction_service.rest.controller.request.TransferMoneyATMRequest;
import com.modernbank.transaction_service.rest.controller.request.WithdrawFromATMRequest;
import com.modernbank.transaction_service.rest.controller.response.BaseResponse;
import com.modernbank.transaction_service.rest.service.event.IWithdrawFromATMServiceProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j

public class WithdrawFromATMServiceProducerImpl implements IWithdrawFromATMServiceProducer {

    private final KafkaTemplate<String, TransferMoneyATMRequest> transferMoneyKafkaTemplate;

    private final KafkaTemplate<String, WithdrawFromATMRequest> withdrawMoneyKafkaTemplate;

    @Override
    public BaseResponse transferMoneyATM(TransferMoneyATMRequest request) {
        log.info("Sending withdraw money request to Kafka topic");
        transferMoneyKafkaTemplate.send("deposit-money-to-atm", request);
        return new BaseResponse("Withdraw money from atm request sent successfully");
    }

    public BaseResponse withdrawMoneyFromATM(WithdrawFromATMRequest request){
        log.info("Sending withdraw money request to Kafka topic");
        withdrawMoneyKafkaTemplate.send("withdraw-money-from-atm", request);
        return new BaseResponse("Withdraw money from atm request sent successfully");
    }
}