package com.modernbank.transaction_service.exception;

public class InsufficientFundsException extends BusinessException {

  public InsufficientFundsException(String message, Object ...args) {
    super(message, args);
  }
}