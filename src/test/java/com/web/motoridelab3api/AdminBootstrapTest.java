package com.web.motoridelab3api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.web.motoridelab3api.model.User;
import com.web.motoridelab3api.repository.UserRepository;
import com.web.motoridelab3api.security.AdminBootstrap;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

class AdminBootstrapTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final ApplicationContextRunner context =
            new ApplicationContextRunner()
                    .withUserConfiguration(AdminBootstrap.class)
                    .withBean(UserRepository.class, () -> users)
                    .withBean(PasswordEncoder.class, () -> passwords)
                    .withPropertyValues(
                            "app.bootstrap.email=bootstrap@example.org",
                            "app.bootstrap.password=Bootstrap-test-password");

    @Test
    void bootstrapAndUserAreEnabledByDefault() {
        when(passwords.encode(anyString())).thenReturn("encoded-password");
        context.run(
                application -> {
                    application
                            .getBean(AdminBootstrap.class)
                            .run(new DefaultApplicationArguments());
                    var saved = ArgumentCaptor.forClass(User.class);
                    verify(users).saveAndFlush(saved.capture());
                    assertTrue(saved.getValue().isEnabled());
                    assertEquals(User.Role.ADMIN, saved.getValue().getRole());
                    assertEquals("encoded-password", saved.getValue().getPasswordHash());
                });
    }

    @Test
    void initializationCanBeDisabled() {
        context.withPropertyValues("app.bootstrap.enabled=false")
                .run(
                        application ->
                                assertTrue(
                                        application
                                                .getBeansOfType(AdminBootstrap.class)
                                                .isEmpty()));
    }

    @Test
    void bootstrapCanCreateAnInactiveUser() {
        when(passwords.encode(anyString())).thenReturn("encoded-password");
        context.withPropertyValues("app.bootstrap.user-enabled=false")
                .run(
                        application -> {
                            application
                                    .getBean(AdminBootstrap.class)
                                    .run(new DefaultApplicationArguments());
                            var saved = ArgumentCaptor.forClass(User.class);
                            verify(users).saveAndFlush(saved.capture());
                            assertFalse(saved.getValue().isEnabled());
                        });
    }

    @Test
    void existingAccountIsNotOverwrittenOrPromoted() {
        var user = new User();
        when(users.findByEmail("bootstrap@example.org")).thenReturn(Optional.of(user));
        context.run(
                application -> {
                    application
                            .getBean(AdminBootstrap.class)
                            .run(new DefaultApplicationArguments());
                    verify(users, never()).saveAndFlush(any());
                    verifyNoInteractions(passwords);
                    assertEquals(User.Role.USER, user.getRole());
                });
    }
}
