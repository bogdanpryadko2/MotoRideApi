package com.web.motoridelab3api.dto;

import java.time.LocalDateTime;

public record FavoriteResponse(
        Long id,
        MotorcycleSummaryResponse motorcycle,
        LocalDateTime createdAt) {
}
