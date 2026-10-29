package com.web.motoridelab3api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;

public record MotorcycleRequest(
        @NotBlank @Size(max = 100) String brand,
        @NotBlank @Size(max = 100) String model,
        @NotNull @Positive @Max(10000) Integer engineCc,
        @NotNull @Positive @Max(2000) Integer horsepower,
        @Size(max = 100) String badge,
        @Size(max = 255) String shortDescription,
        @NotBlank @Size(max = 50000) String description,
        @NotBlank
                @Size(max = 500)
                @Pattern(regexp = "^(https?://[^\\s/]+/[^\\s]*|/images/[^\\s]*)$")
                String imageUrl,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @NotNull @Positive Long categoryId,
        @NotNull @Size(max = 100) Set<@NotNull @Positive Long> technologyIds) {}
