package com.web.motoridelab3api.security;

import com.web.motoridelab3api.dto.ApiErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SecurityErrors {
    private final ObjectMapper mapper;

    public void write(HttpServletRequest request, HttpServletResponse response, int status)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
        if ("HEAD".equals(request.getMethod())) return;
        mapper.writeValue(
                response.getOutputStream(),
                new ApiErrorResponse(
                        status,
                        HttpStatus.valueOf(status).getReasonPhrase(),
                        status == 401 ? "Authentication required" : "Access denied",
                        request.getRequestURI(),
                        LocalDateTime.now(Clock.systemUTC()),
                        null));
    }
}
