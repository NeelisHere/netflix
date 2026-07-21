package com.netflix.video_service.exception;

import com.netflix.common_lib.dto.exception.CommonException;
import com.netflix.common_lib.dto.exception.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ErrorResponse> handleCommonException(CommonException e) {
        ErrorResponse errorResponse = new ErrorResponse(
                e.getMessage(),
                e.getHttpStatus().toString(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(e.getHttpStatus()).body(errorResponse);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxSize(MaxUploadSizeExceededException e) {
        ErrorResponse errorResponse = new ErrorResponse(
                e.getMessage(),
                HttpStatus.CONTENT_TOO_LARGE.toString(),
                LocalDateTime.now()
        );
        return ResponseEntity
                .status(HttpStatus.CONTENT_TOO_LARGE)
                .body(errorResponse);
    }
}
