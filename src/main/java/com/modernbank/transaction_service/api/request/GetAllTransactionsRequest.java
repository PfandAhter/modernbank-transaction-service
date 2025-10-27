package com.modernbank.transaction_service.api.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter

public class GetAllTransactionsRequest extends BaseRequest{
    private String accountId;
    private int page;
    private int size;

    private String type;
    private String dateRange;
}