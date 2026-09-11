package com.netflix.streaming_service.exception;

import com.netflix.common_lib.dto.exception.CommonException;
import com.netflix.common_lib.dto.exception.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = CommonException.class)
    public ResponseEntity<ErrorResponse> handleCommonException(CommonException e) {
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(new ErrorResponse(e.getMessage(), e.getHttpStatus().toString(), LocalDateTime.now()));
    }
}
