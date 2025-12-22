package com.modernbank.transaction_service.service.util;

import com.modernbank.transaction_service.api.client.AccountServiceClient;
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

    public void validateSufficientFunds(String iban, double amount) {
        GetAccountByIban account = getAccountOrThrow(iban, DYNAMIC_ACCOUNT_NOT_FOUND);
        validateSufficientFunds(account, amount);
    }

    @Override
    public void validateUserOwnership(String userId, String fromIBAN) {
        GetAccountByIban account = getAccountOrThrow(fromIBAN, DYNAMIC_ACCOUNT_NOT_FOUND);

        if (!account.getUserId().equals(userId)) {
            throw new BusinessException(DYNAMIC_USER_NOT_ACCOUNT_OWNER, fromIBAN);
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