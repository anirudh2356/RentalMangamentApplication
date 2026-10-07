package com.realestate.profile;

import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.profile.dto.PasswordChangeRequest;
import com.realestate.profile.dto.ProfileUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileServiceTest {

    private final UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ProfileService profileService = new ProfileService(userRepository, passwordEncoder);
    private User broker;

    @BeforeEach
    void setUp() {
        broker = new User(27L, "Broker Name");
        broker.setEmail("broker@example.local");
        broker.setRole(Role.BROKER);
        broker.setEnabled(true);
        broker.setCreatedAt(Instant.parse("2025-01-02T03:04:05Z"));
        when(userRepository.findByEmailIgnoreCase("broker@example.local")).thenReturn(Optional.of(broker));
    }

    @Test
    void returnsOnlySafeAuthenticatedBrokerProfileFields() {
        var profile = profileService.get("broker@example.local");

        assertEquals(27L, profile.id());
        assertEquals("Broker Name", profile.name());
        assertEquals("broker@example.local", profile.email());
        assertEquals(Role.BROKER, profile.role());
        assertEquals(true, profile.enabled());
        assertEquals(Instant.parse("2025-01-02T03:04:05Z"), profile.createdAt());
    }

    @Test
    void updatesAuthenticatedBrokerNameAndNormalizedEmailOnly() {
        when(userRepository.findByEmailIgnoreCase("new@example.local")).thenReturn(Optional.empty());
        when(userRepository.save(broker)).thenReturn(broker);

        var updated = profileService.update(
                "broker@example.local",
                new ProfileUpdateRequest(" Updated Name ", " New@Example.Local ")
        );

        assertEquals("Updated Name", updated.name());
        assertEquals("new@example.local", updated.email());
        assertEquals(Role.BROKER, broker.getRole());
        assertEquals(true, broker.isEnabled());
        assertEquals(27L, broker.getId());
        verify(userRepository).save(broker);
    }

    @Test
    void allowsKeepingOwnEmailButRejectsDuplicateEmailCaseInsensitively() {
        when(userRepository.findByEmailIgnoreCase("BROKER@example.local")).thenReturn(Optional.of(broker));
        when(userRepository.save(broker)).thenReturn(broker);

        assertEquals("broker@example.local", profileService.update(
                "broker@example.local",
                new ProfileUpdateRequest("Broker Name", "BROKER@example.local")
        ).email());

        User other = new User(28L, "Other Broker");
        other.setEmail("other@example.local");
        other.setRole(Role.BROKER);
        when(userRepository.findByEmailIgnoreCase("other@example.local")).thenReturn(Optional.of(other));
        assertEquals(409, assertThrows(
                ResponseStatusException.class,
                () -> profileService.update("broker@example.local",
                        new ProfileUpdateRequest("Broker Name", " Other@Example.Local "))
        ).getStatusCode().value());
    }

    @Test
    void changesPasswordWithCorrectCurrentPasswordAndStoresOnlyBcryptHash() {
        broker.setPasswordHash(passwordEncoder.encode("current-password"));
        when(userRepository.save(broker)).thenReturn(broker);

        profileService.changePassword(
                "broker@example.local",
                new PasswordChangeRequest("current-password", "a-strong-new-password")
        );

        assertNotEquals("a-strong-new-password", broker.getPasswordHash());
        assertTrue(broker.getPasswordHash().startsWith("$2"));
        assertTrue(passwordEncoder.matches("a-strong-new-password", broker.getPasswordHash()));
        verify(userRepository).save(broker);
    }

    @Test
    void rejectsWrongCurrentPasswordSamePasswordAndBcryptOversizedPassword() {
        broker.setPasswordHash(passwordEncoder.encode("current-password"));

        assertEquals(400, assertThrows(
                ResponseStatusException.class,
                () -> profileService.changePassword("broker@example.local",
                        new PasswordChangeRequest("wrong-password", "a-strong-new-password"))
        ).getStatusCode().value());
        assertEquals(400, assertThrows(
                ResponseStatusException.class,
                () -> profileService.changePassword("broker@example.local",
                        new PasswordChangeRequest("current-password", "current-password"))
        ).getStatusCode().value());
        assertEquals(400, assertThrows(
                ResponseStatusException.class,
                () -> profileService.changePassword("broker@example.local",
                        new PasswordChangeRequest("current-password", "é".repeat(40)))
        ).getStatusCode().value());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void onlyEnabledBrokersCanReadOrUpdateProfile() {
        broker.setRole(Role.BUYER);
        assertEquals(403, assertThrows(
                ResponseStatusException.class,
                () -> profileService.get("broker@example.local")
        ).getStatusCode().value());

        broker.setRole(Role.BROKER);
        broker.setEnabled(false);
        assertEquals(403, assertThrows(
                ResponseStatusException.class,
                () -> profileService.update("broker@example.local",
                        new ProfileUpdateRequest("Broker", "broker@example.local"))
        ).getStatusCode().value());
    }
}
