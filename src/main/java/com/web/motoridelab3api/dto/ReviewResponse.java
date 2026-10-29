package com.web.motoridelab3api.dto;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Integer rating,
        String text,
        LocalDateTime createdAt,
        Long authorId,
        String authorName) {}
