package com.web.motoridelab3api.service;

import com.web.motoridelab3api.dto.AuthDtos.Login;
import com.web.motoridelab3api.dto.AuthDtos.PasswordChange;
import com.web.motoridelab3api.dto.AuthDtos.Profile;
import com.web.motoridelab3api.dto.AuthDtos.ProfileUpdate;
import com.web.motoridelab3api.dto.AuthDtos.Register;
import com.web.motoridelab3api.dto.AuthDtos.Tokens;
import com.web.motoridelab3api.exception.UnauthorizedException;
import com.web.motoridelab3api.model.RefreshSession;
import com.web.motoridelab3api.model.User;
import com.web.motoridelab3api.repository.RefreshSessionRepository;
import com.web.motoridelab3api.repository.UserRepository;
import com.web.motoridelab3api.security.AuthProperties;
import com.web.motoridelab3api.security.CurrentUser;
import com.web.motoridelab3api.security.TokenHasher;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final RefreshSessionRepository sessions;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final AuthProperties properties;
    private final CurrentUser current;
    private final EntityManager entityManager;
    private final SecureRandom random = new SecureRandom();

    private String dummyHash;

    @PostConstruct
    void init() {
        dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    public record Result(Tokens body, String refreshToken, Instant refreshExpiresAt) {}

    @Transactional
    public Result register(Register r) {
        var user = new User();
        user.setEmail(normalize(r.email()));
        user.setDisplayName(r.displayName().strip());
        user.setPasswordHash(passwords.encode(r.password()));
        user.setRole(User.Role.USER);
        users.saveAndFlush(user);
        return issue(user, UUID.randomUUID(), Instant.now().plus(properties.refreshTtl()));
    }

    @Transactional
    public Result login(Login r) {
        var found = users.findByEmail(normalize(r.email()));

        User user = found.map(value -> lock(value.getId())).orElse(null);
        boolean matches =
                passwords.matches(r.password(), user == null ? dummyHash : user.getPasswordHash());
        if (user == null || !matches || !user.isEnabled()) throw denied();
        return issue(user, UUID.randomUUID(), Instant.now().plus(properties.refreshTtl()));
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Result refresh(String raw) {
        if (raw == null || raw.length() > 200) throw denied();
        User user =
                lock(
                        sessions.findUserIdByTokenHash(TokenHasher.hash(raw))
                                .orElseThrow(AuthService::denied));

        var session =
                sessions.findByTokenHash(TokenHasher.hash(raw)).orElseThrow(AuthService::denied);
        if (session.getRevokedAt() != null) {
            sessions.revokeFamily(session.getFamilyId(), Instant.now());
            throw denied();
        }
        if (!user.isEnabled() || !session.getExpiresAt().isAfter(Instant.now())) throw denied();
        session.setRevokedAt(Instant.now());
        sessions.saveAndFlush(session);
        return issue(user, session.getFamilyId(), session.getExpiresAt());
    }

    @Transactional
    public void logout(String raw) {
        if (raw == null || raw.isBlank()) return;
        var candidate = sessions.findByTokenHash(TokenHasher.hash(raw));
        if (candidate.isPresent()) {
            lock(candidate.get().getUser().getId());
            sessions.revokeFamily(candidate.get().getFamilyId(), Instant.now());
        }
    }

    @Transactional
    public void logoutAll() {
        var user = lock(current.get().getId());
        sessions.revokeAll(user.getId(), Instant.now());
    }

    @Transactional
    public void changePassword(PasswordChange r) {
        User user = lock(current.get().getId());
        if (!passwords.matches(r.currentPassword(), user.getPasswordHash())) throw denied();
        user.setPasswordHash(passwords.encode(r.newPassword()));
        sessions.revokeAll(user.getId(), Instant.now());
    }

    @Transactional(readOnly = true)
    public Profile me() {
        return profile(current.get());
    }

    @Transactional
    public Profile update(ProfileUpdate r) {
        var user = lock(current.get().getId());
        user.setDisplayName(r.displayName().strip());
        users.saveAndFlush(user);
        return profile(user);
    }

    private Result issue(User user, UUID family, Instant expires) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var session = new RefreshSession();
        session.setUser(user);
        session.setFamilyId(family);
        session.setTokenHash(TokenHasher.hash(raw));
        session.setExpiresAt(expires);
        sessions.saveAndFlush(session);
        Instant now = Instant.now();
        var claims =
                JwtClaimsSet.builder()
                        .issuer(properties.issuer())
                        .subject(user.getId().toString())
                        .issuedAt(now)
                        .expiresAt(now.plus(properties.accessTtl()))
                        .id(UUID.randomUUID().toString())
                        .claim("role", user.getRole().name())
                        .build();
        String access =
                encoder.encode(
                                JwtEncoderParameters.from(
                                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                        .getTokenValue();
        return new Result(
                new Tokens(access, "Bearer", properties.accessTtl().toSeconds(), profile(user)),
                raw,
                expires);
    }

    private User lock(Long id) {
        var user = users.lockById(id).orElseThrow(AuthService::denied);
        entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        return user;
    }

    private static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static UnauthorizedException denied() {
        return new UnauthorizedException("Invalid credentials or session");
    }

    private Profile profile(User user) {
        return new Profile(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
