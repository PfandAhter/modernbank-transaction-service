package com.modernbank.transaction_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modernbank.transaction_service.api.response.BaseResponse;
import com.modernbank.transaction_service.exception.RemoteDirectException;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class CustomFeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        String sourceService = "UnknownService";
        if (methodKey != null && methodKey.contains("#")) {
            sourceService = methodKey.split("#")[0];
        }

        String responseBody = "";
        BaseResponse errorResponse = null;

        try {
            if (response.body() != null) {
                responseBody = IOUtils.toString(response.body().asInputStream(), StandardCharsets.UTF_8);

                errorResponse = objectMapper.readValue(responseBody, BaseResponse.class);
            }
        } catch (IOException e) {
            log.warn("[{}] servisinden gelen cevap okunamadı veya JSON değil. Body: {}", sourceService, responseBody);
        }

        if (errorResponse != null) {
            String remoteCode = errorResponse.getProcessCode();
            String remoteMsg = errorResponse.getProcessMessage();

            log.error("[{}] Servisinden Hata Alındı! Status: {}, Kod: {}, Mesaj: {}",
                    sourceService, response.status(), remoteCode, remoteMsg);

//            if (response.status() >= 500) {
//                return new RemoteSystemException("Karşı serviste sistem hatası: " + remoteMsg);
//            }

            return new RemoteDirectException(remoteCode, remoteMsg, response.status());
        }
        log.error("[{}] Servisine Ulaşılamıyor veya Geçersiz Cevap! Status: {}", sourceService, response.status());
        return new RetryableException(
                response.status(),
                sourceService + " servisi şu an yanıt vermiyor.",
                response.request().httpMethod(),
                (Long) null,
                response.request()
        );
    }
}
