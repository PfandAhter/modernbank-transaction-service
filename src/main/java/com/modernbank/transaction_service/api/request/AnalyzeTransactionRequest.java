package com.modernbank.transaction_service.api.request;

import com.modernbank.transaction_service.model.enums.AnalyzeRange;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnalyzeTransactionRequest extends BaseRequest{
    private AnalyzeRange analyzeRange;
}