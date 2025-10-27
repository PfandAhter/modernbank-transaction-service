package com.modernbank.transaction_service.service.scheduler;

import com.modernbank.transaction_service.entity.ATMTransfer;
import com.modernbank.transaction_service.model.enums.ATMTransferStatus;
import com.modernbank.transaction_service.repository.ATMTransferRepository;
import com.modernbank.transaction_service.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheScheduler {

    private final ATMTransferRepository atmTransferRepository;

    private final RefundService refundService;

    @Scheduled(cron = "0 0 0 * * *") // Her gün gece 00:00'da çalışır
    public void cancelExpiredTransactions() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);

        List<ATMTransfer> expiredATMTransfers = atmTransferRepository
                .findAllByStatusAndCreatedAtBefore(ATMTransferStatus.PENDING, threshold);

        for (ATMTransfer atmTransfer : expiredATMTransfers) {
            atmTransfer.setStatus(ATMTransferStatus.CANCELED);
            atmTransferRepository.save(atmTransfer);

            refundService.refundMoneyToAccountFromATM(atmTransfer);
        }
    }
}
