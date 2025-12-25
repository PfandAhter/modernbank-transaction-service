package com.modernbank.transaction_service.api.response;

import com.modernbank.transaction_service.api.dto.AccountDTO;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GetAccountsResponse extends BaseResponse{
    private List<AccountDTO> accounts;
    private String firstName;
    private String secondName;
    private String lastName;
}
