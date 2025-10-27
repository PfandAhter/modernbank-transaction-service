package com.modernbank.transaction_service.exception;

import com.modernbank.transaction_service.api.client.ParameterServiceClient;
import com.modernbank.transaction_service.api.request.LogErrorRequest;
import com.modernbank.transaction_service.api.response.BaseResponse;
import com.modernbank.transaction_service.entity.ErrorCodes;
import com.modernbank.transaction_service.service.ErrorCacheService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final ErrorCacheService errorCacheService;

    private final ParameterServiceClient parameterServiceClient;

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<BaseResponse> handleException(NotFoundException e, HttpServletRequest request){
        logError(e);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponseBody(e,request));
    }

    @ExceptionHandler(ErrorCodesNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    public ResponseEntity<BaseResponse> handleException(ErrorCodesNotFoundException e, HttpServletRequest request){
        logError(e);
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(createErrorResponseBody(e,request));
    }

    @ExceptionHandler(ProcessFailedException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    public ResponseEntity<BaseResponse> handleException(ProcessFailedException e, HttpServletRequest request){
        logError(e);
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(createErrorResponseBody(e,request));
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    public ResponseEntity<BaseResponse> handleException(RuntimeException e, HttpServletRequest request){
        logError(e);
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(createErrorResponseBody(e,request));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    public ResponseEntity<BaseResponse> handleException(Exception e, HttpServletRequest request){
        logError(e);
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(createErrorResponseBody(e,request));
    }

    //TODO: Buraya HTTPSTATUS kodlarini da parametre servisine loglama icin gondermesini sagliyalim...
    private BaseResponse createErrorResponseBody(Exception exception, HttpServletRequest request){
        ErrorCodes errorCodes = getErrorCodeByErrorId(exception.getMessage());
        logError(exception,request.getHeader("X-User-Id"));

        return new BaseResponse("FAILED", errorCodes.getError(), errorCodes.getDescription());
    }

    private ErrorCodes getErrorCodeByErrorId(String code){
        return errorCacheService.getErrorCodeByErrorId(code);
    }

    private void logError(Exception exception, String userId){
        try{
            LogErrorRequest request = LogErrorRequest.builder()
                    .errorCode(exception.getMessage())
                    .serviceName("authentication-service")
                    .timestamp(LocalDateTime.now().toString())
                    .stackTrace(exception.getStackTrace().toString())
                    .exceptionName(exception.getClass().getName())
                    .build();

            request.setUserId(userId);
            parameterServiceClient.logError(request);
        }catch (Exception e){
            log.error("Error log process failed " + e.getMessage());
        }
    }

    private void logError(Exception exception){
        log.error("Error: " + exception.getMessage());
    }
}