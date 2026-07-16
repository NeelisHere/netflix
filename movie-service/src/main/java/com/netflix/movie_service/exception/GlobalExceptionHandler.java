package com.netflix.movie_service.exception;

import com.netflix.common_lib.dto.exception.CommonException;
import com.netflix.common_lib.dto.exception.ErrorResponse;
import com.netflix.common_lib.dto.exception.MovieNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MovieNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMovieNotFoundException(MovieNotFoundException e) {
        ErrorResponse errorResponse = new ErrorResponse(
                e.getMessage(),
                e.getHttpStatus().toString(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(e.getHttpStatus()).body(errorResponse);
    }

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ErrorResponse> handleCommonException(CommonException e) {
        ErrorResponse errorResponse = new ErrorResponse(
                e.getMessage(),
                e.getHttpStatus().toString(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(e.getHttpStatus()).body(errorResponse);
    }
}
