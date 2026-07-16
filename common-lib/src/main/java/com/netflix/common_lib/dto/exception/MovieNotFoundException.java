package com.netflix.common_lib.dto.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.util.UUID;

@Getter
public class MovieNotFoundException extends RuntimeException {
    private final HttpStatus httpStatus;

    public MovieNotFoundException(HttpStatus httpStatus, UUID movieId) {
        super(String.format("movie: %s not found!", movieId.toString()));
        this.httpStatus = httpStatus;
    }
}
