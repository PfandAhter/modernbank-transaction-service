package com.modernbank.transaction_service.service.util;

import com.modernbank.transaction_service.api.client.AccountServiceClient;
import com.modernbank.transaction_service.api.request.TransferMoneyATMRequest;
import com.modernbank.transaction_service.api.request.TransferMoneyRequest;
import com.modernbank.transaction_service.api.request.WithdrawAndDepositMoneyRequest;
import com.modernbank.transaction_service.api.response.GetAccountByIban;
import com.modernbank.transaction_service.api.response.GetAccountByIdResponse;
import com.modernbank.transaction_service.exception.BusinessException;
import com.modernbank.transaction_service.exception.InsufficientFundsException;
import com.modernbank.transaction_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.modernbank.transaction_service.constant.ErrorCodeConstants.*;

@Service
@RequiredArgsConstructor
public class TransactionValidatorImpl implements TransactionValidator {

    private final AccountServiceClient accountServiceClient;

    private final TransactionRepository transactionRepository;

    @Override
    public void validateTransferMoney(TransferMoneyRequest request) {
        GetAccountByIban fromAccount = getAccountByIBANOrThrow(request.getFromIBAN());
        validateSufficientFunds(fromAccount, request.getAmount());
        validateUserOwnership(request.getUserId(), fromAccount, request.getFromIBAN());
        isSenderIBANBlacklisted(request.getFromIBAN());
        isReceiverIBANBlacklisted(request.getToIBAN());
        validateIBANsAreDifferent(request.getFromIBAN(), request.getToIBAN());
    }

    @Override
    public void validateDepositMoneyDailyLimit(WithdrawAndDepositMoneyRequest request) {
        isAccountBlocked(request.getAccountId());
        validateDepositDailyLimit(request);
    }

    @Override
    public void validateWithdrawMoneyDailyLimit(WithdrawAndDepositMoneyRequest request) {
        isAccountBlocked(request.getAccountId());
        GetAccountByIdResponse account = getAccountByIdOrThrow(request.getAccountId());
        validateSufficientFunds(account, request.getAmount());
        validateWithdrawDailyLimit(request, account);
    }

    @Override
    public void validateDepositMoneyATMLimit(TransferMoneyATMRequest request){
        GetAccountByIban senderAccount = getAccountByIBANOrThrow(request.getSenderIban());
        isAccountBlocked(senderAccount.getAccountId());
        validateSufficientFunds(senderAccount, request.getAmount());

        Double depositATMDailySum = transactionRepository
                .sumDepositsToATMLast24Hours(senderAccount.getAccountId(), LocalDateTime.now());

        if (depositATMDailySum + request.getAmount() > senderAccount.getDailyDepositLimit()) {
            throw new BusinessException(DYNAMIC_ATM_DEPOSIT_LIMIT_EXCEEDED,
                    senderAccount.getDailyDepositLimit(),
                    senderAccount.getDailyDepositLimit() - depositATMDailySum
            );
        }
    }

    private void validateIBANsAreDifferent(String fromIBAN, String toIBAN) {
        if (fromIBAN.equals(toIBAN)) {
            throw new BusinessException(DYNAMIC_SAME_IBAN_TRANSFER,
                    fromIBAN,
                    toIBAN
            );
        }
    }

    private void validateDepositDailyLimit(WithdrawAndDepositMoneyRequest request) {
        GetAccountByIdResponse account = getAccountByIdOrThrow(request.getAccountId());
        double last24HourDepositSum = transactionRepository.
                sumDepositsLast24Hours(request.getAccountId(), LocalDateTime.now());

        if (last24HourDepositSum + request.getAmount() > account.getAccount().getDailyDepositLimit()) {
            throw new BusinessException(DYNAMIC_DAILY_DEPOSIT_LIMIT_EXCEEDED,
                    account.getAccount().getDailyDepositLimit(),
                    account.getAccount().getDailyDepositLimit() - last24HourDepositSum
            );
        }
    }

    private void validateWithdrawDailyLimit(WithdrawAndDepositMoneyRequest request, GetAccountByIdResponse account) {
        double last24HourWithdrawSum = transactionRepository.
                sumWithdrawalsLast24Hours(request.getAccountId(), LocalDateTime.now());

        if (last24HourWithdrawSum + request.getAmount() > account.getAccount().getDailyWithdrawLimit()) {
            throw new BusinessException(DYNAMIC_DAILY_WITHDRAW_LIMIT_EXCEEDED,
                    account.getAccount().getDailyWithdrawLimit(),
                    account.getAccount().getDailyWithdrawLimit() - last24HourWithdrawSum
            );
        }
    }


    private void validateUserOwnership(String userId, GetAccountByIban account, String fromIBAN) {
        isAccountBlocked(account.getAccountId());

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

    private void isSenderIBANBlacklisted(String iban) {
        boolean isBlacklisted = accountServiceClient.isReceiverBlacklisted(iban);

        if (isBlacklisted) {
            throw new BusinessException(DYNAMIC_SENDER_IBAN_BLACKLISTED, iban);
        }
    }

    private void isAccountBlocked(String accountId) {
        boolean isBlocked = accountServiceClient.isAccountBlocked(accountId);

        if (isBlocked) {
            throw new BusinessException(DYNAMIC_ACCOUNT_BLOCKED);
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

    private void validateSufficientFunds(GetAccountByIdResponse account, double amount) {
        if (account.getAccount().getBalance() < amount) {
            throw new InsufficientFundsException(
                    DYNAMIC_INSUFFICIENT_FUNDS,
                    account.getAccount().getBalance(),
                    amount
            );
        }
    }

    private GetAccountByIban getAccountByIBANOrThrow(String iban) {
        GetAccountByIban account = accountServiceClient.getAccountByIban(iban);

        if (account == null) {
            throw new BusinessException(DYNAMIC_ACCOUNT_NOT_FOUND, iban);
        }

        return account;
    }

    private GetAccountByIdResponse getAccountByIdOrThrow(String id) {
        GetAccountByIdResponse accountResponse = accountServiceClient.getAccountById(id);

        if (accountResponse == null || accountResponse.getAccount() == null) {
            throw new BusinessException(DYNAMIC_ACCOUNT_NOT_FOUND, id);
        }

        return accountResponse;
    }
}