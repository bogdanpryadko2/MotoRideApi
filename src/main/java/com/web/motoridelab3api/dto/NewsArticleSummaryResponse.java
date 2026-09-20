package com.web.motoridelab3api.dto;

import java.time.LocalDateTime;

public record NewsArticleSummaryResponse(
        Long id,
        String title,
        String imageUrl,
        LocalDateTime publishedAt) {
}
