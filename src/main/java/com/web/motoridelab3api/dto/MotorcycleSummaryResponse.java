package com.web.motoridelab3api.dto;

import java.math.BigDecimal;

public record MotorcycleSummaryResponse(
        Long id,
        String brand,
        String model,
        Integer engineCc,
        Integer horsepower,
        String badge,
        String imageUrl,
        BigDecimal price,
        String categorySlug,
        String categoryName) {}
