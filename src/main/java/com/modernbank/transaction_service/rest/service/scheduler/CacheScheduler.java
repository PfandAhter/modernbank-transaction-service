package com.modernbank.transaction_service.rest.service.scheduler;

import com.modernbank.transaction_service.model.entity.ATMTransfer;
import com.modernbank.transaction_service.model.enums.ATMTransferStatus;
import com.modernbank.transaction_service.repository.ATMTransferRepository;
import com.modernbank.transaction_service.rest.service.IErrorCacheService;
import com.modernbank.transaction_service.rest.service.IRefundService;
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

    private final IErrorCacheService errorCacheService;

    private final IRefundService refundService;

    @Scheduled(cron = "${cache.refresh.cronErrorCode:0 0 1 * * SUN}") // Default value is 1 AM every Sunday
    public void refreshErrorCodesCache(){
        log.info("Refreshing error codes cache");
        try{
            errorCacheService.refreshErrorCodesCache();
            log.info("Error codes cache refreshed successfully");
        }catch (Exception e){
            log.error("Error occurred while refreshing error codes cache", e);
        }
    }

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
