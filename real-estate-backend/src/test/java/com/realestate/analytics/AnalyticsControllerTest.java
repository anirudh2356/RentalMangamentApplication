package com.realestate.analytics;

import com.realestate.analytics.dto.AnalyticsOverviewResponse;
import com.realestate.messaging.config.SecurityConfig;
import com.realestate.messaging.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
@Import(SecurityConfig.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void returnsAuthenticatedBrokersOverview() throws Exception {
        when(analyticsService.overview("broker@example.local")).thenReturn(new AnalyticsOverviewResponse(
                7, 3, 2, 1, 1, Map.of("NEW", 3L),
                5, 3, 2, Map.of("ACTIVE", 3L),
                2, 4, 2, 1, 1, Map.of("NEGOTIATION", 2L),
                new BigDecimal("1500000.00"), new BigDecimal("30000.00"),
                new BigDecimal("30000.00"), false
        ));

        mockMvc.perform(get("/api/analytics/overview")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLeads").value(7))
                .andExpect(jsonPath("$.newLeads").value(3))
                .andExpect(jsonPath("$.totalProperties").value(5))
                .andExpect(jsonPath("$.dealPipeline.NEGOTIATION").value(2))
                .andExpect(jsonPath("$.pendingCommission").value(30000.0));

        verify(analyticsService).overview("broker@example.local");
    }

    @Test
    void deniesNonBrokerOverviewAccess() throws Exception {
        mockMvc.perform(get("/api/analytics/overview")
                        .with(user("buyer@example.local").roles("BUYER")))
                .andExpect(status().isForbidden());
    }
}
