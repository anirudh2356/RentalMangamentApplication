package com.realestate.settings;

import com.realestate.messaging.config.SecurityConfig;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.settings.dto.SettingsResponse;
import com.realestate.settings.dto.SettingsUpdateRequest;
import com.realestate.settings.model.DashboardSection;
import com.realestate.settings.model.ThemePreference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SettingsController.class)
@Import({SecurityConfig.class, SettingsExceptionHandler.class})
class SettingsControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private SettingsService settingsService;
    @MockitoBean private UserRepository userRepository;

    @Test
    void authenticatedBrokerCanGetSettingsCreatedForTheirPrincipal() throws Exception {
        when(settingsService.get("broker@example.local")).thenReturn(defaults());

        mockMvc.perform(get("/api/settings")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailNotificationsEnabled").value(true))
                .andExpect(jsonPath("$.defaultDashboardSection").value("OVERVIEW"))
                .andExpect(jsonPath("$.theme").value("SYSTEM"))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist());

        verify(settingsService).get("broker@example.local");
    }

    @Test
    void unauthenticatedAndNonBrokerAccessAreRejected() throws Exception {
        mockMvc.perform(get("/api/settings")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/settings")
                        .with(user("buyer@example.local").roles("BUYER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/settings")
                        .with(user("buyer@example.local").roles("BUYER"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(validJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateRequiresCsrfAndOnlyUsesAuthenticatedOwner() throws Exception {
        when(settingsService.update(eq("broker@example.local"), any(SettingsUpdateRequest.class)))
                .thenReturn(new SettingsResponse(
                        false, true, false, true, false, true,
                        DashboardSection.CALENDAR, ThemePreference.DARK,
                        Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-07T00:00:00Z")
                ));

        mockMvc.perform(put("/api/settings")
                        .with(user("broker@example.local").roles("BROKER"))
                        .contentType("application/json")
                        .content(validJson()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/settings")
                        .with(user("broker@example.local").roles("BROKER"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(validJson().replace(
                                "\"emailNotificationsEnabled\": true",
                                "\"userId\": 999, \"brokerId\": 888, \"ownerId\": 777,\n"
                                        + "\"role\": \"ADMIN\", \"enabled\": false,\n"
                                        + "\"emailNotificationsEnabled\": true"
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Settings request contains invalid or malformed values"));
        verify(settingsService, never()).update(eq("broker@example.local"), any(SettingsUpdateRequest.class));

        mockMvc.perform(put("/api/settings")
                        .with(user("broker@example.local").roles("BROKER"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(validJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("DARK"))
                .andExpect(jsonPath("$.defaultDashboardSection").value("CALENDAR"));

        verify(settingsService).update(eq("broker@example.local"), any(SettingsUpdateRequest.class));
    }

    @Test
    void anotherBrokerCanOnlyRequestTheirOwnSettings() throws Exception {
        when(settingsService.get("second@example.local")).thenReturn(new SettingsResponse(
                false, false, false, false, false, false,
                DashboardSection.LEADS, ThemePreference.LIGHT,
                Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-07T00:00:00Z")
        ));

        mockMvc.perform(get("/api/settings")
                        .with(user("second@example.local").roles("BROKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leadNotificationsEnabled").value(false))
                .andExpect(jsonPath("$.defaultDashboardSection").value("LEADS"));

        verify(settingsService).get("second@example.local");
    }

    @Test
    void settingsResponseDoesNotExposeOwnerOrAccountFields() throws Exception {
        when(settingsService.get("broker@example.local")).thenReturn(defaults());

        mockMvc.perform(get("/api/settings")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void rejectsInvalidEnumValuesAndMissingPreferences() throws Exception {
        mockMvc.perform(put("/api/settings")
                        .with(user("broker@example.local").roles("BROKER"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(validJson().replace("\"DARK\"", "\"NEON\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Settings request contains invalid or malformed values"));

        mockMvc.perform(put("/api/settings")
                        .with(user("broker@example.local").roles("BROKER"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(validJson().replace("\"OVERVIEW\"", "\"MESSAGES\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Settings request contains invalid or malformed values"));

        mockMvc.perform(put("/api/settings")
                        .with(user("broker@example.local").roles("BROKER"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"theme":"LIGHT","defaultDashboardSection":"OVERVIEW"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Settings validation failed"))
                .andExpect(jsonPath("$.fields.emailNotificationsEnabled").exists());
    }

    private static SettingsResponse defaults() {
        return new SettingsResponse(true, true, true, true, true, true,
                DashboardSection.OVERVIEW, ThemePreference.SYSTEM,
                Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-01T00:00:00Z"));
    }

    private static String validJson() {
        return """
                {
                  "emailNotificationsEnabled": true,
                  "leadNotificationsEnabled": true,
                  "siteVisitNotificationsEnabled": true,
                  "dealNotificationsEnabled": true,
                  "followUpNotificationsEnabled": true,
                  "documentNotificationsEnabled": true,
                  "defaultDashboardSection": "OVERVIEW",
                  "theme": "DARK"
                }
                """;
    }
}
