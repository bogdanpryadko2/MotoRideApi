package com.web.motoridelab3api.security;

import com.web.motoridelab3api.exception.UnauthorizedException;
import com.web.motoridelab3api.model.User;
import com.web.motoridelab3api.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class CurrentUser {
    private final UserRepository users;

    public User get() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt))
            throw new UnauthorizedException("Authentication required");
        return users.findById(Long.valueOf(jwt.getSubject()))
                .filter(User::isEnabled)
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    }

    public void requireOwnerOrAdmin(Long ownerId) {
        var user = get();
        if (user.getRole() != User.Role.ADMIN && !Objects.equals(ownerId, user.getId()))
            throw new AccessDeniedException("Resource belongs to another user");
    }
}
