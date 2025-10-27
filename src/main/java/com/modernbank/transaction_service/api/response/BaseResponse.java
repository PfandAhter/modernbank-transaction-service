package com.modernbank.transaction_service.api.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class BaseResponse {
    private String status = "1";

    private String processCode = "H-0001";

    private String processMessage = "SUCCESS";

    public BaseResponse(String processMessage){
        this.processMessage = processMessage;
    }

    public BaseResponse(String processCode, String processMessage){
        this.processCode = processCode;
        this.processMessage = processMessage;
    }
}