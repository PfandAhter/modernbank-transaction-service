package com.modernbank.transaction_service.repository;

import com.modernbank.transaction_service.model.entity.ATMTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ATMTransferRepository extends JpaRepository<ATMTransfer, String> {
    // ATMTransferRepository methods can be defined here
    // For example, you can add custom query methods if needed

    @Query("SELECT a FROM ATMTransfer a WHERE (a.receiverIban = ?1 OR a.receiverTckn = ?1) AND a.atmId = ?2 AND a.active = 1")
    List<Optional<ATMTransfer>> findATMTransferByReceiverIbanOrReceiverTckn(String receiverIbanOrTckn, String atmId);

    @Query("SELECT a FROM ATMTransfer a WHERE (a.receiverIban = ?1 OR a.receiverTckn = ?1) AND a.atmId = ?2 AND a.active = 1")
    List<ATMTransfer> findATMTransferByReceiverIbanOrReceiverTcknAndActive(String receiverIbanOrTckn, String atmId);
}
