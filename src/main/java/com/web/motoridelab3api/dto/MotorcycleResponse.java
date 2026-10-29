package com.web.motoridelab3api.dto;

import java.math.BigDecimal;
import java.util.List;

public record MotorcycleResponse(
        Long id,
        String brand,
        String model,
        Integer engineCc,
        Integer horsepower,
        String badge,
        String imageUrl,
        BigDecimal price,
        String categorySlug,
        String categoryName,
        String shortDescription,
        String description,
        List<String> technologies,
        Long categoryId,
        List<Long> technologyIds) {}
