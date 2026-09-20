package com.web.motoridelab3api.dto;

import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(int status, String message, String details, String path, LocalDateTime timestamp, Map<String, String> fieldErrors) {
}