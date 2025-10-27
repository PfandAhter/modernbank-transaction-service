package com.modernbank.transaction_service.api.response;

import com.modernbank.transaction_service.api.dto.TransactionDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GetTransactionsResponse extends BaseResponse{

    private List<TransactionDTO> transactions;

    private Long totalElements;

    private int totalPages;
}