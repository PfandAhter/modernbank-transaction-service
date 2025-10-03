package com.modernbank.transaction_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionListModel {
    private List<TransactionModel> transactions;
    private int totalPages;
    private long totalElements;

}