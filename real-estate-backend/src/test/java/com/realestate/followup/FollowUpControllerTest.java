package com.realestate.followup;

import com.realestate.followup.dto.FollowUpResponse;
import com.realestate.followup.model.FollowUpPriority;
import com.realestate.followup.model.FollowUpStatus;
import com.realestate.messaging.config.SecurityConfig;
import com.realestate.messaging.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FollowUpController.class)
@Import({SecurityConfig.class, FollowUpExceptionHandler.class})
class FollowUpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FollowUpService service;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void brokerCanCreateFollowUpWithCsrfAndAuthenticatedIdentity() throws Exception {
        when(service.create(eq("broker@example.local"), any())).thenReturn(response());

        mockMvc.perform(post("/api/followups")
                        .with(user("broker@example.local").roles("BROKER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(31))
                .andExpect(jsonPath("$.leadName").value("Buyer"));

        verify(service).create(eq("broker@example.local"), any());
    }

    @Test
    void followUpWritesRequireCsrf() throws Exception {
        mockMvc.perform(post("/api/followups")
                        .with(user("broker@example.local").roles("BROKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonBrokerCannotAccessFollowUps() throws Exception {
        mockMvc.perform(get("/api/followups")
                        .with(user("buyer@example.local").roles("BUYER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidFollowUpDataReturnsCleanValidationError() throws Exception {
        mockMvc.perform(post("/api/followups")
                        .with(user("broker@example.local").roles("BROKER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","leadId":null,"followUpAt":null,"status":null,"priority":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Follow-up validation failed"))
                .andExpect(jsonPath("$.fields.title").exists())
                .andExpect(jsonPath("$.fields.leadId").exists());
    }

    private static String validRequest() {
        return """
                {"title":"Call buyer","description":"Discuss listings","leadId":5,
                 "followUpAt":"2026-10-08T12:30:00Z","status":"PENDING","priority":"HIGH"}
                """;
    }

    private static FollowUpResponse response() {
        return new FollowUpResponse(
                31L, "Call buyer", "Discuss listings", 5L, "Buyer",
                Instant.parse("2026-10-08T12:30:00Z"), FollowUpStatus.PENDING,
                FollowUpPriority.HIGH, null, null
        );
    }
}
