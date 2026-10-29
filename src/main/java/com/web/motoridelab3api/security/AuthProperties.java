package com.web.motoridelab3api.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties("app.auth")
@Validated
public record AuthProperties(
        @NotBlank String jwtSecret,
        @NotBlank String issuer,
        @NotNull Duration accessTtl,
        @NotNull Duration refreshTtl,
        boolean cookieSecure,
        @Pattern(regexp = "Strict|Lax|None") String cookieSameSite,
        @NotEmpty List<String> allowedOrigins) {
    public AuthProperties {
        if (accessTtl != null && (accessTtl.isNegative() || accessTtl.isZero()))
            throw new IllegalArgumentException("Invalid access TTL");
        if (refreshTtl != null && (refreshTtl.isNegative() || refreshTtl.isZero()))
            throw new IllegalArgumentException("Invalid refresh TTL");
        if ("None".equals(cookieSameSite) && !cookieSecure)
            throw new IllegalArgumentException("SameSite=None requires Secure");
    }
}
