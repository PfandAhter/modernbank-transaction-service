package com.modernbank.transaction_service.api.response;

import com.modernbank.transaction_service.api.dto.AccountDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetAccountByIdResponse {
    AccountDTO account;
}