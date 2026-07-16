package com.netflix.common_lib.dto.exception;

import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;


public record ErrorResponse(String message, String httpStatus, LocalDateTime timestamp) { }
