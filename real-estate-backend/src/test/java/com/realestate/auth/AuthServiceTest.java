package com.realestate.auth;

import com.realestate.auth.dto.AuthenticatedUserResponse;
import com.realestate.auth.dto.LoginRequest;
import com.realestate.messaging.config.SecurityConfig;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AuthService authService;

    @BeforeEach
    void setUp() {
        SecurityConfig securityConfig = new SecurityConfig();
        authService = new AuthService(
                new ProviderManager(securityConfig.authenticationProvider(
                        securityConfig.userDetailsService(userRepository),
                        passwordEncoder
                )),
                userRepository
        );
    }

    @Test
    void authenticatesAndReturnsUserProfile() {
        User alice = user(1L, "Alice", "alice@example.local", Role.BUYER);
        alice.setPasswordHash(passwordEncoder.encode("development-password"));
        when(userRepository.findByEmailIgnoreCase("alice@example.local"))
                .thenReturn(Optional.of(alice));

        Authentication authentication = authService.authenticate(
                new LoginRequest(" alice@example.local ", "development-password")
        );
        AuthenticatedUserResponse profile = authService.getUser(authentication.getName());

        assertEquals("alice@example.local", authentication.getName());
        assertEquals(1L, profile.id());
        assertEquals("Alice", profile.name());
        assertEquals("alice@example.local", profile.email());
        assertEquals(Role.BUYER, profile.role());
    }

    @Test
    void rejectsInvalidPassword() {
        User alice = user(1L, "Alice", "alice@example.local", Role.BUYER);
        alice.setPasswordHash(passwordEncoder.encode("correct-password"));
        when(userRepository.findByEmailIgnoreCase("alice@example.local"))
                .thenReturn(Optional.of(alice));

        assertThrows(
                BadCredentialsException.class,
                () -> authService.authenticate(
                        new LoginRequest("alice@example.local", "incorrect")
                )
        );
    }

    @Test
    void rejectsUnknownEmail() {
        assertThrows(
                BadCredentialsException.class,
                () -> authService.authenticate(
                        new LoginRequest("unknown@example.local", "any-password")
                )
        );
    }

    @Test
    void rejectsDisabledUser() {
        User alice = user(1L, "Alice", "alice@example.local", Role.BUYER);
        alice.setPasswordHash(passwordEncoder.encode("development-password"));
        alice.setEnabled(false);
        when(userRepository.findByEmailIgnoreCase("alice@example.local"))
                .thenReturn(Optional.of(alice));

        assertThrows(
                DisabledException.class,
                () -> authService.authenticate(
                        new LoginRequest("alice@example.local", "development-password")
                )
        );
    }

    @Test
    void bcryptStoresOnlyOneWayPasswordHashes() {
        String password = "development-password";
        String hash = passwordEncoder.encode(password);

        assertNotEquals(password, hash);
        assertTrue(passwordEncoder.matches(password, hash));
        assertTrue(hash.startsWith("$2"));
    }

    private User user(Long id, String name, String email, Role role) {
        User user = new User(id, name);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }
}
