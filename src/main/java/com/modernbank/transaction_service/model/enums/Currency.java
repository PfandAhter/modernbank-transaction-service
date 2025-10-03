package com.modernbank.transaction_service.model.enums;

public enum Currency {
    TRY("TRY"),
    USD("USD"),
    EURO("EURO"),
    GOLD("GOLD"),
    SILVER("SILVER");

    private final String currency;

    Currency(String currency) {
        this.currency = currency;
    }

    public String getCurrency() {
        return currency;
    }
}