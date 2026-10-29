package com.web.motoridelab3api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record NewsArticleRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 100000) String content,
        @Size(max = 500) @Pattern(regexp = "^(https?://[^\\s/]+/[^\\s]*|/images/[^\\s]*)$")
                String imageUrl,
        @NotNull LocalDateTime publishedAt) {}
