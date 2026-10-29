package com.web.motoridelab3api.security;

import com.web.motoridelab3api.model.User;
import com.web.motoridelab3api.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
@ConditionalOnProperty(
        prefix = "app.bootstrap",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder passwords;

    @Value("${app.bootstrap.email:}")
    private String email;

    @Value("${app.bootstrap.password:}")
    private String password;

    @Value("${app.bootstrap.user-enabled:true}")
    private boolean userEnabled;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) return;
        if (email.isBlank()
                || password.length() < 16
                || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalStateException("Bootstrap requires email and password of 16–72 bytes");
        String normalized = email.strip().toLowerCase(Locale.ROOT);
        if (users.findByEmail(normalized).isPresent()) return;
        var user = new User();
        user.setEmail(normalized);
        user.setDisplayName("Administrator");
        user.setPasswordHash(passwords.encode(password));
        user.setRole(User.Role.ADMIN);
        user.setEnabled(userEnabled);
        users.saveAndFlush(user);
    }
}
