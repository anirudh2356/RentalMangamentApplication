package com.realestate.reports;

import com.realestate.messaging.config.SecurityConfig;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.reports.dto.CommissionReport;
import com.realestate.reports.dto.DealReport;
import com.realestate.reports.dto.LeadReport;
import com.realestate.reports.dto.PropertyReport;
import com.realestate.reports.dto.ReportOverview;
import com.realestate.reports.dto.SiteVisitReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportsController.class)
@Import({SecurityConfig.class, ReportsExceptionHandler.class})
class ReportsControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ReportService reportService;
    @MockitoBean private UserRepository userRepository;

    @Test
    void brokerCanAccessAllReportTypesAndOwnershipComesFromPrincipal() throws Exception {
        when(reportService.overview("broker@example.local", null, null)).thenReturn(overview());
        when(reportService.leads("broker@example.local", null, null, null)).thenReturn(new LeadReport(0, Map.of(), Map.of(), List.of()));
        when(reportService.properties("broker@example.local", null, null, null)).thenReturn(propertyReport());
        when(reportService.siteVisits("broker@example.local", null, null, null)).thenReturn(siteVisitReport());
        when(reportService.deals("broker@example.local", null, null, null)).thenReturn(dealReport());
        when(reportService.commissions("broker@example.local", null, null)).thenReturn(commissionReport());

        mockMvc.perform(get("/api/reports/overview?brokerId=999&userId=888&ownerId=777")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLeads").value(4));
        mockMvc.perform(get("/api/reports/leads").with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/reports/properties").with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/reports/site-visits").with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/reports/deals").with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/reports/commissions").with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk());

        verify(reportService).overview("broker@example.local", null, null);
        verify(reportService).leads("broker@example.local", null, null, null);
        verify(reportService).properties("broker@example.local", null, null, null);
        verify(reportService).siteVisits("broker@example.local", null, null, null);
        verify(reportService).deals("broker@example.local", null, null, null);
        verify(reportService).commissions("broker@example.local", null, null);
    }

    @Test
    void unauthenticatedAndNonBrokerReportAccessAreRejected() throws Exception {
        mockMvc.perform(get("/api/reports/overview")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/reports/overview")
                        .with(user("buyer@example.local").roles("BUYER")))
                .andExpect(status().isForbidden());
        verify(reportService, never()).overview("buyer@example.local", null, null);
    }

    @Test
    void dateAndStatusFiltersAreParsedAndInvalidRangesRejected() throws Exception {
        when(reportService.leads(
                "broker@example.local",
                java.time.LocalDate.parse("2026-10-01"),
                java.time.LocalDate.parse("2026-10-02"),
                "QUALIFIED"
        )).thenReturn(new LeadReport(0, Map.of(), Map.of(), List.of()));

        mockMvc.perform(get("/api/reports/leads")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-02")
                        .param("status", "QUALIFIED")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reports/leads")
                        .param("from", "not-a-date")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid report filter"));

        mockMvc.perform(get("/api/reports/leads")
                        .param("from", "2026-10-03")
                        .param("to", "2026-10-02")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("'from' date must be on or before 'to'"));
    }

    @Test
    void clientSuppliedOwnerFiltersNeverReachService() throws Exception {
        when(reportService.deals("broker@example.local", null, null, null)).thenReturn(dealReport());

        mockMvc.perform(get("/api/reports/deals")
                        .param("brokerId", "999")
                        .param("userId", "888")
                        .param("ownerId", "777")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk());

        verify(reportService).deals("broker@example.local", null, null, null);
        verify(reportService, never()).deals("other@example.local", null, null, null);
    }

    @Test
    void csvExportsHaveAttachmentHeadersAndOnlyContainServiceReturnedBrokerRows() throws Exception {
        when(reportService.leads("broker@example.local", null, null, null)).thenReturn(new LeadReport(
                1, Map.of("QUALIFIED", 1L), Map.of("REFERRAL", 1L),
                List.of(new LeadReport.LeadRow(9L, "Lead, \"North\"\nDistrict", "REFERRAL",
                        "QUALIFIED", "Loft", Instant.parse("2026-10-02T10:00:00Z")))
        ));

        mockMvc.perform(get("/api/reports/leads/export")
                        .param("brokerId", "999")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"Lead, \"\"North\"\"\nDistrict\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("other broker"))));

        verify(reportService).leads("broker@example.local", null, null, null);
    }

    @Test
    void csvExportWithNoRowsStillHasHeader() throws Exception {
        when(reportService.properties("broker@example.local", null, null, null)).thenReturn(propertyReport());

        mockMvc.perform(get("/api/reports/properties/export")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.startsWith(
                        "\"Property ID\",\"Title\",\"Type\",\"Listing Type\",\"Status\",\"Available\",\"Price\",\"City\",\"Created At\"")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("properties-report.csv")));
    }

    private static ReportOverview overview() {
        return new ReportOverview(4, 1, 1, 3, 2, 1, 2, 1, 1,
                new BigDecimal("500000"), new BigDecimal("10000"), new BigDecimal("10000"));
    }

    private static PropertyReport propertyReport() {
        return new PropertyReport(0, 0, 0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                Map.of(), Map.of(), Map.of(), List.of());
    }

    private static SiteVisitReport siteVisitReport() {
        return new SiteVisitReport(0, 0, 0, 0, Map.of(), Map.of(), Map.of(), List.of());
    }

    private static DealReport dealReport() {
        return new DealReport(0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                Map.of(), Map.of(), Map.of(), List.of());
    }

    private static CommissionReport commissionReport() {
        return new CommissionReport(BigDecimal.ZERO, BigDecimal.ZERO, Map.of(), List.of());
    }
}
