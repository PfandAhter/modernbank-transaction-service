package com.modernbank.transaction_service.model;


import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionAnalyzeModel {

    private List<EnrichedTransaction> transactions;
}