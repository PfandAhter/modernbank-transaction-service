package com.modernbank.transaction_service.rest.controller.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter

public class GetAllTransactionsRequest extends BaseRequest{
    private String accountId;
    private int page;
    private int size;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private String type;
    private String dateRange;
}