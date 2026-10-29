package com.web.motoridelab3api.dto;

import com.web.motoridelab3api.validation.PasswordLength;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuthDtos {
    private AuthDtos() {}

    public record Register(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @PasswordLength String password,
            @NotBlank @Size(max = 100) String displayName) {}

    public record Login(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @PasswordLength(min = 1) String password) {}

    public record ProfileUpdate(@NotBlank @Size(max = 100) String displayName) {}

    public record PasswordChange(
            @NotBlank @PasswordLength(min = 1) String currentPassword,
            @NotBlank @PasswordLength String newPassword) {}

    public record Profile(
            Long id,
            String email,
            String displayName,
            String role,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt) {}

    public record Tokens(String accessToken, String tokenType, long expiresIn, Profile user) {}
}
