package com.web.motoridelab3api.dto;

import java.time.LocalDateTime;

public record NewsArticleResponse(
        Long id,
        String title,
        String content,
        String imageUrl,
        LocalDateTime publishedAt) {
}
