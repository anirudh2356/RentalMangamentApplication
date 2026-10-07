package com.realestate.settings;

import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.settings.dto.SettingsUpdateRequest;
import com.realestate.settings.model.DashboardSection;
import com.realestate.settings.model.Settings;
import com.realestate.settings.model.ThemePreference;
import com.realestate.settings.repository.SettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettingsServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final SettingsRepository settingsRepository = mock(SettingsRepository.class);
    private final SettingsService settingsService = new SettingsService(userRepository, settingsRepository);
    private User broker;

    @BeforeEach
    void setUp() {
        broker = new User(61L, "Broker");
        broker.setEmail("broker@example.local");
        broker.setRole(Role.BROKER);
        broker.setEnabled(true);
        when(userRepository.findByEmailIgnoreCase("broker@example.local")).thenReturn(Optional.of(broker));
    }

    @Test
    void firstGetCreatesBrokerSettingsWithSensibleDefaults() {
        when(settingsRepository.findByUserId(61L)).thenReturn(Optional.empty());
        when(settingsRepository.save(org.mockito.ArgumentMatchers.any(Settings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = settingsService.get("broker@example.local");

        assertTrue(response.emailNotificationsEnabled());
        assertTrue(response.leadNotificationsEnabled());
        assertTrue(response.siteVisitNotificationsEnabled());
        assertTrue(response.dealNotificationsEnabled());
        assertTrue(response.followUpNotificationsEnabled());
        assertTrue(response.documentNotificationsEnabled());
        assertEquals(DashboardSection.OVERVIEW, response.defaultDashboardSection());
        assertEquals(ThemePreference.SYSTEM, response.theme());
        verify(settingsRepository).save(org.mockito.ArgumentMatchers.argThat(settings ->
                settings.getUser() == broker));
    }

    @Test
    void getsSettingsByAuthenticatedBrokerRatherThanClientProvidedOwner() {
        Settings brokerSettings = settingsFor(broker);
        brokerSettings.setDefaultDashboardSection(DashboardSection.LEADS);
        User otherBroker = new User(62L, "Other");
        otherBroker.setEmail("other@example.local");
        otherBroker.setRole(Role.BROKER);
        Settings otherSettings = settingsFor(otherBroker);
        otherSettings.setDefaultDashboardSection(DashboardSection.DEALS);

        when(settingsRepository.findByUserId(61L)).thenReturn(Optional.of(brokerSettings));
        when(userRepository.findByEmailIgnoreCase("other@example.local")).thenReturn(Optional.of(otherBroker));
        when(settingsRepository.findByUserId(62L)).thenReturn(Optional.of(otherSettings));

        assertEquals(DashboardSection.LEADS,
                settingsService.get("broker@example.local").defaultDashboardSection());
        assertEquals(DashboardSection.DEALS,
                settingsService.get("other@example.local").defaultDashboardSection());
        assertEquals(broker, brokerSettings.getUser());
        assertEquals(otherBroker, otherSettings.getUser());
    }

    @Test
    void updatesOnlySettingsOwnedByAuthenticatedBroker() {
        Settings existing = settingsFor(broker);
        when(settingsRepository.findByUserId(61L)).thenReturn(Optional.of(existing));
        when(settingsRepository.save(existing)).thenReturn(existing);
        SettingsUpdateRequest request = new SettingsUpdateRequest(
                false, true, false, true, false, true,
                DashboardSection.CALENDAR, ThemePreference.DARK
        );

        var updated = settingsService.update("broker@example.local", request);

        assertFalse(updated.emailNotificationsEnabled());
        assertTrue(updated.leadNotificationsEnabled());
        assertFalse(updated.siteVisitNotificationsEnabled());
        assertTrue(updated.dealNotificationsEnabled());
        assertFalse(updated.followUpNotificationsEnabled());
        assertTrue(updated.documentNotificationsEnabled());
        assertEquals(DashboardSection.CALENDAR, updated.defaultDashboardSection());
        assertEquals(ThemePreference.DARK, updated.theme());
        assertEquals(broker, existing.getUser());
        verify(settingsRepository).save(existing);
        verify(settingsRepository, never()).findByUserId(62L);
    }

    @Test
    void createsSettingsOnFirstUpdateWithoutAcceptingOwnershipFromRequest() {
        when(settingsRepository.findByUserId(61L)).thenReturn(Optional.empty());
        when(settingsRepository.save(org.mockito.ArgumentMatchers.any(Settings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        SettingsUpdateRequest request = new SettingsUpdateRequest(
                true, true, true, true, true, true,
                DashboardSection.OVERVIEW, ThemePreference.SYSTEM
        );

        settingsService.update("broker@example.local", request);

        verify(settingsRepository).save(org.mockito.ArgumentMatchers.argThat(settings ->
                settings.getUser() == broker));
    }

    @Test
    void deniesDisabledOrNonBrokerUsers() {
        broker.setRole(Role.BUYER);
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> settingsService.get("broker@example.local")).getStatusCode().value());

        broker.setRole(Role.BROKER);
        broker.setEnabled(false);
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> settingsService.update("broker@example.local", new SettingsUpdateRequest(
                        true, true, true, true, true, true,
                        DashboardSection.OVERVIEW, ThemePreference.SYSTEM
                ))).getStatusCode().value());
    }

    private Settings settingsFor(User owner) {
        Settings settings = new Settings();
        settings.setUser(owner);
        return settings;
    }
}
