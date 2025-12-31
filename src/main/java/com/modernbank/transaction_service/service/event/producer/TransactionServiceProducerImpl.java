package com.modernbank.transaction_service.service.event.producer;


import com.modernbank.transaction_service.api.client.AccountServiceClient;
import com.modernbank.transaction_service.api.request.TransferMoneyRequest;
import com.modernbank.transaction_service.api.request.WithdrawAndDepositMoneyRequest;
import com.modernbank.transaction_service.api.response.BaseResponse;
import com.modernbank.transaction_service.api.response.GetAccountByIban;
import com.modernbank.transaction_service.exception.NotFoundException;
import com.modernbank.transaction_service.service.event.ITransactionServiceProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j

public class TransactionServiceProducerImpl implements ITransactionServiceProducer {

    private final KafkaTemplate<String, WithdrawAndDepositMoneyRequest> withdrawAndDepositMoneyKafkaTemplate;

    private final KafkaTemplate<String, TransferMoneyRequest> transferMoneyKafkaTemplate;

    private final AccountServiceClient accountServiceClient;

    @Override
    public BaseResponse withdrawMoney(WithdrawAndDepositMoneyRequest request) {
        log.info("Sending withdraw money request to Kafka topic");
        withdrawAndDepositMoneyKafkaTemplate.send("withdraw-money", request);
        return new BaseResponse("Para çekme talebi başarıyla gönderildi");
    }

    @Override
    public BaseResponse depositMoney(WithdrawAndDepositMoneyRequest request) {
        log.info("Sending deposit money request to Kafka topic");
        withdrawAndDepositMoneyKafkaTemplate.send("deposit-money", request);
        return new BaseResponse("Para yatırma talebi başarıyla gönderildi");
    }

    @Override
    public BaseResponse transferMoney(TransferMoneyRequest request) {
        log.info("Received transfer money request CONFIRMATION: {}", request.getIsConfirmed());
        if (Boolean.FALSE.equals(request.getIsConfirmed())) {
            log.info("Validation request received for: {}", request);

            GetAccountByIban sender = accountServiceClient.getAccountByIban(request.getFromIBAN());
            if (sender == null) {
                throw new NotFoundException("Gönderici hesap bulunamadı.");
            }

            GetAccountByIban receiver = accountServiceClient.getAccountByIban(request.getToIBAN());
            if (receiver == null) {
                throw new NotFoundException("Alıcı hesap bulunamadı. Lütfen IBAN'ı kontrol ediniz.");
            }

            boolean nameMatch = isNameMatch(receiver, request); // Aşağıda helper metod yaptım
            if (!nameMatch) {
                // AI'ın bunu kullanıcıya söyleyebilmesi için hata fırlatıyoruz
                throw new IllegalArgumentException("Girilen Alıcı Adı ile Hesap Sahibi Adı uyuşmuyor! Gerçek Hesap Sahibi: "
                        + receiver.getFirstName().substring(0,2) + "***");
            }

            if (sender.getBalance() < request.getAmount()) {
                throw new IllegalArgumentException("Yetersiz bakiye! Mevcut bakiyeniz: " + sender.getBalance());
            }

            return new BaseResponse("VALIDATION_SUCCESS",
                    String.format("Alıcı: %s %s doğrulanmıştır. İşlem ücreti yoktur. Onaylıyor musunuz?",
                            receiver.getFirstName(), receiver.getLastName()));
        }

        log.info("Sending transfer money request to Kafka topic (Confirmed)");

        // Artık güvenli, Kafka akışını başlat.
        // Not: KafkaListener içinde tekrar kontrol olması güvenlik açısından iyidir, kalabilir.
        transferMoneyKafkaTemplate.send("start-transfer-money", request);

        return new BaseResponse("H-0001", "Transfer işlemi başlatıldı.");
    }


    private boolean isNameMatch(GetAccountByIban account, TransferMoneyRequest request) {
        // İsim kontrol mantığın (Null check eklemeyi unutma)
        if (request.getToFirstName() == null) return true;

        return account.getFirstName().equalsIgnoreCase(request.getToFirstName()) &&
                account.getLastName().equalsIgnoreCase(request.getToLastName());
    }
}