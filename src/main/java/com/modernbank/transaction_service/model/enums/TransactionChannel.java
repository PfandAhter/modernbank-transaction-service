package com.modernbank.transaction_service.model.enums;

public enum TransactionChannel {
    ATM("ATM"),
    ONLINE_BANKING("ONLINE_BANKING"),
    AUTOMATIC("AUTOMATIC"),
    MOBILE_APP("MOBILE_APP"),
    BRANCH("BRANCH"),
    POS("POS"),
    PHONE_BANKING("PHONE_BANKING"),
    CHEQUE("CHEQUE"),
    WIRE_TRANSFER("WIRE_TRANSFER"),
    DIRECT_DEPOSIT("DIRECT_DEPOSIT"),
    BILL_PAYMENT("BILL_PAYMENT"),
    OTHER("OTHER");

    private final String channel;

    TransactionChannel(String channel) {
        this.channel = channel;
    }

    public String getChannel() {
        return channel;
    }
}
