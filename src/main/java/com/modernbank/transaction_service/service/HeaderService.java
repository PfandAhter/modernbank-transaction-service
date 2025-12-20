package com.modernbank.transaction_service.service;

public interface HeaderService {
    String extractToken();

    String extractUserEmail();

    String extractUserId();

    String extractUserRole();

    String extractCorrelationId();
}