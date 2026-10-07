package com.realestate.profile;

import com.realestate.messaging.config.SecurityConfig;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.profile.dto.ProfileResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.SecurityContextRepository;
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

@WebMvcTest(ProfileController.class)
@Import({SecurityConfig.class, ProfileExceptionHandler.class})
class ProfileControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ProfileService profileService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private SecurityContextRepository securityContextRepository;

    @Test
    void authenticatedBrokerCanReadSafeProfile() throws Exception {
        when(profileService.get("broker@example.local")).thenReturn(profile());

        mockMvc.perform(get("/api/profile")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(27))
                .andExpect(jsonPath("$.email").value("broker@example.local"))
                .andExpect(jsonPath("$.role").value("BROKER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void profileRequiresAuthenticatedBroker() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/profile")
                        .with(user("buyer@example.local").roles("BUYER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void profileUpdateRequiresCsrfAndCannotBindSensitiveFields() throws Exception {
        when(profileService.update(eq("broker@example.local"), any())).thenReturn(
                new ProfileResponse(27L, "Updated Broker", "new@example.local", Role.BROKER,
                        true, Instant.parse("2025-01-02T03:04:05Z"))
        );

        mockMvc.perform(put("/api/profile")
                        .with(user("broker@example.local").roles("BROKER"))
                        .contentType("application/json")
                        .content("""
                                {"name":"Updated Broker","email":"new@example.local"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/profile")
                        .with(user("broker@example.local").roles("BROKER"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"id":1,"name":"Updated Broker","email":"new@example.local",
                                 "role":"ADMIN","enabled":false,"passwordHash":"attacker"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(27))
                .andExpect(jsonPath("$.role").value("BROKER"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(profileService).update(eq("broker@example.local"), any());
        verify(securityContextRepository, org.mockito.Mockito.atLeastOnce())
                .saveContext(any(SecurityContext.class), any(), any());
    }

    @Test
    void emailUpdatePersistsNewPrincipalInSessionContext() throws Exception {
        when(profileService.update(eq("broker@example.local"), any())).thenReturn(
                new ProfileResponse(27L, "Broker", "updated@example.local", Role.BROKER,
                        true, Instant.parse("2025-01-02T03:04:05Z"))
        );

        mockMvc.perform(put("/api/profile")
                        .with(user("broker@example.local").roles("BROKER"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"name":"Broker","email":"updated@example.local"}
                                """))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<SecurityContext> context =
                org.mockito.ArgumentCaptor.forClass(SecurityContext.class);
        verify(securityContextRepository, org.mockito.Mockito.atLeastOnce())
                .saveContext(context.capture(), any(), any());
        org.junit.jupiter.api.Assertions.assertTrue(context.getAllValues().stream()
                .anyMatch(saved -> "updated@example.local".equals(saved.getAuthentication().getName())));
    }

    @Test
    void validationRejectsBlankNameAndMalformedEmail() throws Exception {
        mockMvc.perform(put("/api/profile")
                        .with(user("broker@example.local").roles("BROKER"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"name":" ","email":"not-an-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Profile validation failed"))
                .andExpect(jsonPath("$.fields.name").exists())
                .andExpect(jsonPath("$.fields.email").exists());

        verify(profileService, never()).update(eq("broker@example.local"), any());
    }

    @Test
    void passwordChangeRequiresCsrfAndKeepsSessionAuthentication() throws Exception {
        mockMvc.perform(put("/api/profile/password")
                        .with(user("broker@example.local").roles("BROKER"))
                        .contentType("application/json")
                        .content("""
                                {"currentPassword":"current-password","newPassword":"new-password-123"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/profile/password")
                        .with(user("broker@example.local").roles("BROKER"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"currentPassword":"current-password","newPassword":"new-password-123"}
                                """))
                .andExpect(status().isNoContent());

        verify(profileService).changePassword(eq("broker@example.local"), any());
    }

    @Test
    void weakPasswordIsRejectedByValidation() throws Exception {
        mockMvc.perform(put("/api/profile/password")
                        .with(user("broker@example.local").roles("BROKER"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"currentPassword":"current-password","newPassword":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.newPassword").exists());
    }

    private static ProfileResponse profile() {
        return new ProfileResponse(
                27L, "Broker", "broker@example.local", Role.BROKER, true,
                Instant.parse("2025-01-02T03:04:05Z")
        );
    }
}
