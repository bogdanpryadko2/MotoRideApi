package com.web.motoridelab3api.dto;

import com.web.motoridelab3api.model.ReactionKind;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public final class SocialDtos {
    private SocialDtos() {}

    public record PostRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 50000) String content,
            @Size(max = 500) @Pattern(regexp = "^(https?://[^\\s/]+/[^\\s]*|/images/[^\\s]*)$")
                    String imageUrl,
            @Positive Long motorcycleId) {}

    public record CommentRequest(@NotBlank @Size(max = 10000) String text) {}

    public record Author(Long id, String displayName) {}

    public record PostResponse(
            Long id,
            String title,
            String content,
            String imageUrl,
            Long motorcycleId,
            Author author,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {}

    public record CommentResponse(
            Long id,
            Long postId,
            String text,
            Author author,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {}

    public record PageResponse<T>(
            List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <T> PageResponse<T> from(Page<T> page) {
            return new PageResponse<>(
                    page.getContent(),
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages());
        }
    }

    public record ReactionResponse(ReactionKind kind) {}

    public record ReactionRequest(@NotNull ReactionKind kind) {}
}
