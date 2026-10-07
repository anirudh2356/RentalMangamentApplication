package com.realestate.notifications;

import com.realestate.messaging.config.SecurityConfig;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.notifications.dto.NotificationResponse;
import com.realestate.notifications.model.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, NotificationExceptionHandler.class})
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private NotificationService notificationService;
    @MockitoBean private UserRepository userRepository;

    @Test
    void authenticatedBrokerCanReadOwnNotificationsAndUnreadCount() throws Exception {
        when(notificationService.list("broker@example.local", true)).thenReturn(List.of(response()));
        when(notificationService.unreadCount("broker@example.local")).thenReturn(1L);

        mockMvc.perform(get("/api/notifications")
                        .param("unreadOnly", "true")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(31))
                .andExpect(jsonPath("$[0].type").value("LEAD"))
                .andExpect(jsonPath("$[0].broker").doesNotExist())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());

        mockMvc.perform(get("/api/notifications/unread-count")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        verify(notificationService).list("broker@example.local", true);
        verify(notificationService).unreadCount("broker@example.local");
    }

    @Test
    void unauthenticatedAndNonBrokerAccessAreRejected() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/notifications")
                        .with(user("buyer@example.local").roles("BUYER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void markReadMarkAllReadAndDeleteRequireCsrf() throws Exception {
        mockMvc.perform(patch("/api/notifications/31/read")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/notifications/read-all")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/notifications/31")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isForbidden());

        when(notificationService.markRead("broker@example.local", 31L)).thenReturn(response());
        when(notificationService.markAllRead("broker@example.local")).thenReturn(3);

        mockMvc.perform(patch("/api/notifications/31/read")
                        .with(user("broker@example.local").roles("BROKER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(31));
        mockMvc.perform(patch("/api/notifications/read-all")
                        .with(user("broker@example.local").roles("BROKER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(3));

        mockMvc.perform(delete("/api/notifications/31")
                        .with(user("broker@example.local").roles("BROKER"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(notificationService).markRead("broker@example.local", 31L);
        verify(notificationService).markAllRead("broker@example.local");
        verify(notificationService).delete("broker@example.local", 31L);
    }

    @Test
    void authenticatedBrokerCanRetrieveOwnNotification() throws Exception {
        when(notificationService.get("broker@example.local", 31L)).thenReturn(response());

        mockMvc.perform(get("/api/notifications/31")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New lead"));

        verify(notificationService).get("broker@example.local", 31L);
    }

    private static NotificationResponse response() {
        return new NotificationResponse(
                31L, "New lead", "A new lead was added.", NotificationType.LEAD,
                false, "LEAD", 22L, Instant.parse("2026-10-07T10:00:00Z")
        );
    }
}
