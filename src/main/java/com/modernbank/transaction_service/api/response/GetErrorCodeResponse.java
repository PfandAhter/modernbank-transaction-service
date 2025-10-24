package com.modernbank.transaction_service.api.response;

import com.modernbank.transaction_service.api.dto.ErrorCodesDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class GetErrorCodeResponse {
    private ErrorCodesDTO errorCode;
}