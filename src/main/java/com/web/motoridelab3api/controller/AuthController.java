package com.web.motoridelab3api.controller;

import com.web.motoridelab3api.dto.AuthDtos.Login;
import com.web.motoridelab3api.dto.AuthDtos.PasswordChange;
import com.web.motoridelab3api.dto.AuthDtos.Register;
import com.web.motoridelab3api.dto.AuthDtos.Tokens;
import com.web.motoridelab3api.security.AuthProperties;
import com.web.motoridelab3api.security.AuthRateLimiter;
import com.web.motoridelab3api.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;
    private final AuthRateLimiter limiter;
    private final AuthProperties properties;

    @GetMapping("/csrf")
    @Operation(
            summary = "Публично: получить CSRF token перед cookie-операциями",
            description =
                    "Передавайте token в заголовке headerName; credentials: include. Refresh"
                            + " хранится только в HttpOnly cookie.")
    public ResponseEntity<Map<String, String>> csrf(CsrfToken token) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("token", token.getToken(), "headerName", token.getHeaderName()));
    }

    @PostMapping("/register")
    public ResponseEntity<Tokens> register(
            @Valid @RequestBody Register r, HttpServletRequest request) {
        limiter.check("register", request.getRemoteAddr(), r.email());
        return response(auth.register(r), true);
    }

    @PostMapping("/login")
    public ResponseEntity<Tokens> login(@Valid @RequestBody Login r, HttpServletRequest request) {
        limiter.check("login", request.getRemoteAddr(), r.email());
        return response(auth.login(r), false);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Ротация refresh cookie",
            description =
                    "Требуется CSRF header. Не отправляйте истёкший Bearer. Повторное использование"
                            + " отзывает всё семейство.")
    public ResponseEntity<Tokens> refresh(
            @CookieValue(name = "refresh_token", required = false) String token) {
        return response(auth.refresh(token), false);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String token) {
        auth.logout(token);
        return cleared();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll() {
        auth.logoutAll();
        return cleared();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody PasswordChange r) {
        auth.changePassword(r);
        return cleared();
    }

    private ResponseEntity<Tokens> response(AuthService.Result result, boolean created) {
        var builder =
                created ? ResponseEntity.created(URI.create("/api/users/me")) : ResponseEntity.ok();
        return builder.cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookie(
                                        result.refreshToken(),
                                        Duration.between(Instant.now(), result.refreshExpiresAt()))
                                .toString())
                .body(result.body());
    }

    private ResponseEntity<Void> cleared() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .cacheControl(CacheControl.noStore())
                .build();
    }

    private ResponseCookie cookie(String value, Duration ttl) {
        return ResponseCookie.from("refresh_token", value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite(properties.cookieSameSite())
                .path("/api/auth")
                .maxAge(ttl)
                .build();
    }
}
