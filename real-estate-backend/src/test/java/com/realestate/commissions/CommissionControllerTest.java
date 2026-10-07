package com.realestate.commissions;

import com.realestate.commissions.dto.CommissionRecordResponse;
import com.realestate.commissions.dto.CommissionSummaryResponse;
import com.realestate.deals.model.DealStatus;
import com.realestate.messaging.config.SecurityConfig;
import com.realestate.messaging.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommissionController.class)
@Import(SecurityConfig.class)
class CommissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommissionService commissionService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void returnsAuthenticatedBrokersCommissionSummary() throws Exception {
        when(commissionService.summary("broker@example.local")).thenReturn(
                new CommissionSummaryResponse(
                        new BigDecimal("12500.00"),
                        new BigDecimal("12500.00"),
                        null,
                        false,
                        List.of(new CommissionRecordResponse(
                                4L,
                                20L,
                                "Alice",
                                30L,
                                "Property A",
                                "Central Road, Pune",
                                new BigDecimal("500000.00"),
                                new BigDecimal("2.50"),
                                new BigDecimal("12500.00"),
                                DealStatus.WON,
                                Instant.parse("2026-10-01T12:00:00Z")
                        ))
                )
        );

        mockMvc.perform(get("/api/commissions")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCommission").value(12500.0))
                .andExpect(jsonPath("$.pendingCommission").value(12500.0))
                .andExpect(jsonPath("$.paymentTrackingSupported").value(false))
                .andExpect(jsonPath("$.commissions[0].dealStatus").value("WON"))
                .andExpect(jsonPath("$.commissions[0].propertyTitle").value("Property A"));

        verify(commissionService).summary("broker@example.local");
    }

    @Test
    void nonBrokerCannotReadCommissionSummary() throws Exception {
        mockMvc.perform(get("/api/commissions")
                        .with(user("buyer@example.local").roles("BUYER")))
                .andExpect(status().isForbidden());
    }
}
