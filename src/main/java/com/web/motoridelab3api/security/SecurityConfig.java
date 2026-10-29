package com.web.motoridelab3api.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.web.motoridelab3api.repository.UserRepository;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Base64;
import java.util.List;
import java.util.Set;

import javax.crypto.spec.SecretKeySpec;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecretKeySpec jwtKey(AuthProperties p) {
        byte[] bytes = Base64.getDecoder().decode(p.jwtSecret());
        if (bytes.length < 32)
            throw new IllegalArgumentException(
                    "JWT_SECRET must contain at least 32 random bytes encoded as base64");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKeySpec key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKeySpec key, AuthProperties p, UserRepository users) {
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> account =
                jwt -> {
                    try {
                        var user = users.findById(Long.valueOf(jwt.getSubject()));
                        if (user.isPresent()
                                && user.get().isEnabled()
                                && user.get().getRole().name().equals(jwt.getClaimAsString("role")))
                            return OAuth2TokenValidatorResult.success();
                    } catch (NumberFormatException ignored) {
                    }
                    return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
                };
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(p.issuer()), account));
        return decoder;
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, AuthProperties p, SecurityErrors errors)
            throws Exception {
        var csrf = new CookieCsrfTokenRepository();
        csrf.setCookieCustomizer(
                c ->
                        c.httpOnly(true)
                                .secure(p.cookieSecure())
                                .sameSite(p.cookieSameSite())
                                .path("/api/auth"));
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(c -> c.disable())
                .addFilterBefore(corsFilter(p, errors), CsrfFilter.class)
                .csrf(
                        c ->
                                c.csrfTokenRepository(csrf)
                                        .requireCsrfProtectionMatcher(
                                                r ->
                                                        r.getRequestURI()
                                                                        .substring(
                                                                                r.getContextPath()
                                                                                        .length())
                                                                        .startsWith("/api/auth/")
                                                                && !Set.of("GET", "HEAD", "OPTIONS")
                                                                        .contains(r.getMethod())))
                .authorizeHttpRequests(
                        a ->
                                a.requestMatchers(
                                                "/swagger-ui.html",
                                                "/swagger-ui/**",
                                                "/v3/api-docs/**",
                                                "/actuator/health")
                                        .permitAll()
                                        .requestMatchers(
                                                "/api/auth/csrf",
                                                "/api/auth/register",
                                                "/api/auth/login",
                                                "/api/auth/refresh",
                                                "/api/auth/logout")
                                        .permitAll()
                                        .requestMatchers(
                                                "/api/favorites/**",
                                                "/api/users/**",
                                                "/api/posts/*/reaction",
                                                "/api/comments/*/reaction",
                                                "/api/reviews/*/reaction")
                                        .authenticated()
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/motorcycles/**",
                                                "/api/categories/**",
                                                "/api/technologies/**",
                                                "/api/news/**",
                                                "/api/posts/**",
                                                "/api/comments/**",
                                                "/api/reviews/**")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.HEAD,
                                                "/api/motorcycles/**",
                                                "/api/categories/**",
                                                "/api/technologies/**",
                                                "/api/news/**",
                                                "/api/posts/**",
                                                "/api/comments/**",
                                                "/api/reviews/**")
                                        .permitAll()
                                        .requestMatchers(
                                                "/api/motorcycles/*/reviews",
                                                "/api/reviews/**",
                                                "/api/posts/**",
                                                "/api/comments/**")
                                        .authenticated()
                                        .requestMatchers(
                                                "/api/motorcycles/**",
                                                "/api/categories/**",
                                                "/api/technologies/**",
                                                "/api/news/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers(
                                                "/api/auth/logout-all", "/api/auth/change-password")
                                        .authenticated()
                                        .anyRequest()
                                        .denyAll())
                .oauth2ResourceServer(
                        o ->
                                o.jwt(j -> j.jwtAuthenticationConverter(converter))
                                        .authenticationEntryPoint(
                                                (r, s, e) -> errors.write(r, s, 401))
                                        .accessDeniedHandler((r, s, e) -> errors.write(r, s, 403)))
                .exceptionHandling(
                        e ->
                                e.authenticationEntryPoint((r, s, x) -> errors.write(r, s, 401))
                                        .accessDeniedHandler((r, s, x) -> errors.write(r, s, 403)))
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .logout(l -> l.disable());
        return http.build();
    }

    private CorsFilter corsFilter(AuthProperties properties, SecurityErrors errors) {
        var processor =
                new DefaultCorsProcessor() {
                    @Override
                    protected void rejectRequest(ServerHttpResponse response) {}
                };
        var filter = new CorsFilter(cors(properties));
        filter.setCorsProcessor(
                (configuration, request, response) -> {
                    boolean accepted = processor.processRequest(configuration, request, response);
                    if (!accepted) errors.write(request, response, 403);
                    return accepted;
                });
        return filter;
    }

    private CorsConfigurationSource cors(AuthProperties p) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(p.allowedOrigins());
        config.setAllowedMethods(List.of("HEAD", "GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN"));
        config.setExposedHeaders(List.of("Location", "X-Total-Count", "X-Total-Pages"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
