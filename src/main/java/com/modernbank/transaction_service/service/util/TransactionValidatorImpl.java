package com.modernbank.transaction_service.service.util;

import com.modernbank.transaction_service.api.client.AccountServiceClient;
import com.modernbank.transaction_service.api.request.TransferMoneyRequest;
import com.modernbank.transaction_service.api.response.GetAccountByIban;
import com.modernbank.transaction_service.exception.BusinessException;
import com.modernbank.transaction_service.exception.InsufficientFundsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.modernbank.transaction_service.constant.ErrorCodeConstants.*;

@Service
@RequiredArgsConstructor
public class TransactionValidatorImpl implements TransactionValidator {

    private final AccountServiceClient accountServiceClient;

    @Override
    public void validateTransferMoney(TransferMoneyRequest request) {
        validateSufficientFunds(request.getFromIBAN(), request.getAmount());
        validateUserOwnership(request.getUserId(), request.getFromIBAN());
        isReceiverIBANBlacklisted(request.getToIBAN());
        validateIBANsAreDifferent(request.getFromIBAN(), request.getToIBAN());
    }

    private void validateIBANsAreDifferent(String fromIBAN, String toIBAN) {
        if (fromIBAN.equals(toIBAN)) {
            throw new BusinessException(DYNAMIC_SAME_IBAN_TRANSFER,
                    fromIBAN,
                    toIBAN
            );
        }
    }

    private void validateSufficientFunds(String iban, double amount) {
        GetAccountByIban account = getAccountOrThrow(iban, DYNAMIC_ACCOUNT_NOT_FOUND);
        validateSufficientFunds(account, amount);
    }

    private void validateUserOwnership(String userId, String fromIBAN) {
        GetAccountByIban account = getAccountOrThrow(fromIBAN, DYNAMIC_ACCOUNT_NOT_FOUND);

        if (!account.getUserId().equals(userId)) {
            throw new BusinessException(DYNAMIC_USER_NOT_ACCOUNT_OWNER, fromIBAN);
        }
    }

    private void isReceiverIBANBlacklisted(String iban) {
        boolean isBlacklisted = accountServiceClient.isReceiverBlacklisted(iban);

        if (isBlacklisted) {
            throw new BusinessException(DYNAMIC_RECEIVER_IBAN_BLACKLISTED, iban);
        }
    }

    private void validateSufficientFunds(GetAccountByIban account, double amount) {
        if (account.getBalance() < amount) {
            throw new InsufficientFundsException(
                    DYNAMIC_INSUFFICIENT_FUNDS,
                    account.getBalance(),
                    amount
            );
        }
    }

    private GetAccountByIban getAccountOrThrow(String iban, String errorMessage) {
        GetAccountByIban account = accountServiceClient.getAccountByIban(iban);

        if(account == null) {
            throw new BusinessException(errorMessage, iban);
        }

        return account;
    }
}