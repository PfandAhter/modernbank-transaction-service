package com.modernbank.transaction_service.exception;

import com.modernbank.transaction_service.model.dto.ErrorCodesDTO;
import com.modernbank.transaction_service.rest.service.IErrorCacheService;
import com.modernbank.transaction_service.rest.service.IMapperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import com.modernbank.transaction_service.rest.controller.response.ErrorResponse;

import java.time.LocalDateTime;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final IErrorCacheService errorCacheService;

    private final IMapperService mapperService;

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleException(NotFoundException e){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createFailResponseBody(e.getMessage(),HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler(ErrorCodesNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    public ResponseEntity<ErrorResponse> handleException(ErrorCodesNotFoundException e){
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(createErrorNotFoundResponseBody(e.getMessage(),HttpStatus.NOT_ACCEPTABLE));
    }

    @ExceptionHandler(ProcessFailedException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    public ResponseEntity<ErrorResponse> handleException(ProcessFailedException e){
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(createFailResponseBody(e.getMessage(),HttpStatus.NOT_ACCEPTABLE));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    public ResponseEntity<ErrorResponse> handleException(Exception e){
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(createFailResponseBody(e.getMessage(),HttpStatus.NOT_ACCEPTABLE));
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    public ResponseEntity<ErrorResponse> handleException(RuntimeException e){
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(createFailResponseBody(e.getMessage(),HttpStatus.NOT_ACCEPTABLE));
    }

    private ErrorResponse createErrorNotFoundResponseBody(String exceptionMessage, HttpStatus status){
        log.error("Error message: {}", exceptionMessage);
        return new ErrorResponse(status, "FAILED", exceptionMessage, LocalDateTime.now());
    }

    private ErrorResponse createFailResponseBody(String exceptionMessage,HttpStatus status){
        log.error("Error message: {}", exceptionMessage);
        ErrorCodesDTO errorCodesDTO = findByErrorCode(exceptionMessage);
        return new ErrorResponse(status, errorCodesDTO.getError(), errorCodesDTO.getDescription(), LocalDateTime.now());
    }

    private ErrorCodesDTO findByErrorCode(String errorId) {
        return mapperService.map(errorCacheService.getErrorCode(errorId), ErrorCodesDTO.class);
    }
}