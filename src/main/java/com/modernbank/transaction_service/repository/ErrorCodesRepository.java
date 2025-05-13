package com.modernbank.transaction_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.modernbank.transaction_service.model.entity.ErrorCodes;

import java.util.Optional;


@Repository
public interface ErrorCodesRepository extends JpaRepository<ErrorCodes, String> {

    @Query("SELECT e FROM ErrorCodes e WHERE e.id = ?1")
    Optional<ErrorCodes> findErrorCodesById(String id);
}
