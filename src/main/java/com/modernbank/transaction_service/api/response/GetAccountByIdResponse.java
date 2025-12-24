package com.modernbank.transaction_service.api.response;

import com.modernbank.transaction_service.api.dto.AccountDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GetAccountByIdResponse {
    AccountDTO account;
}