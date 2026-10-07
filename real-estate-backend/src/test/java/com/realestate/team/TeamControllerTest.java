package com.realestate.team;

import com.realestate.messaging.config.SecurityConfig;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.team.dto.TeamMemberResponse;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamController.class)
@Import(SecurityConfig.class)
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TeamService teamService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void authenticatedBrokerCanReadTeamMembersWithoutSensitiveFields() throws Exception {
        when(teamService.members("broker@example.local", "agent", Role.BUYER, true))
                .thenReturn(List.of(new TeamMemberResponse(
                        34L, "Agent", "agent@example.local", Role.BUYER,
                        true, Instant.parse("2026-10-07T00:00:00Z")
                )));

        mockMvc.perform(get("/api/team/members")
                        .param("search", "agent")
                        .param("role", "BUYER")
                        .param("enabled", "true")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(34))
                .andExpect(jsonPath("$[0].email").value("agent@example.local"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].broker").doesNotExist());

        verify(teamService).members("broker@example.local", "agent", Role.BUYER, true);
    }

    @Test
    void unauthenticatedAccessIsRejected() throws Exception {
        mockMvc.perform(get("/api/team/members"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonBrokerCannotAccessTeamMembers() throws Exception {
        mockMvc.perform(get("/api/team/members")
                        .with(user("buyer@example.local").roles("BUYER")))
                .andExpect(status().isForbidden());
    }
}
