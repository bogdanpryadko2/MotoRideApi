package com.web.motoridelab3api.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiErrorResponse(
        int status,
        String message,
        String details,
        String path,
        LocalDateTime timestamp,
        Map<String, String> fieldErrors) {}
