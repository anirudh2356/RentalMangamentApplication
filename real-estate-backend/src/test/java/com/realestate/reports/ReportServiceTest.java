package com.realestate.reports;

import com.realestate.commissions.CommissionService;
import com.realestate.commissions.dto.CommissionRecordResponse;
import com.realestate.commissions.dto.CommissionSummaryResponse;
import com.realestate.deals.model.Deal;
import com.realestate.deals.model.DealStage;
import com.realestate.deals.model.DealStatus;
import com.realestate.deals.repository.DealRepository;
import com.realestate.leads.model.Lead;
import com.realestate.leads.model.LeadSource;
import com.realestate.leads.model.LeadStatus;
import com.realestate.leads.repository.LeadRepository;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.property.model.ListingType;
import com.realestate.property.model.Property;
import com.realestate.property.model.PropertyStatus;
import com.realestate.property.model.PropertyType;
import com.realestate.property.repository.PropertyRepository;
import com.realestate.reports.dto.CommissionReport;
import com.realestate.reports.dto.DealReport;
import com.realestate.reports.dto.LeadReport;
import com.realestate.reports.dto.PropertyReport;
import com.realestate.reports.dto.ReportOverview;
import com.realestate.reports.dto.SiteVisitReport;
import com.realestate.sitevisit.model.SiteVisit;
import com.realestate.sitevisit.model.SiteVisitStatus;
import com.realestate.sitevisit.repository.SiteVisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final LeadRepository leads = mock(LeadRepository.class);
    private final PropertyRepository properties = mock(PropertyRepository.class);
    private final SiteVisitRepository visits = mock(SiteVisitRepository.class);
    private final DealRepository deals = mock(DealRepository.class);
    private final CommissionService commissionService = mock(CommissionService.class);
    private final ReportService reportService =
            new ReportService(users, leads, properties, visits, deals, commissionService);
    private User broker;
    private Lead lead;
    private Property property;
    private SiteVisit visit;
    private Deal wonDeal;
    private Deal activeDeal;

    @BeforeEach
    void setUp() {
        broker = new User(7L, "Broker");
        broker.setEmail("broker@example.local");
        broker.setRole(Role.BROKER);
        broker.setEnabled(true);
        when(users.findByEmailIgnoreCase("broker@example.local")).thenReturn(Optional.of(broker));

        property = new Property();
        property.setId(11L);
        property.setTitle("Sunny Apartment, \"North\"\nFloor");
        property.setPropertyType(PropertyType.APARTMENT);
        property.setListingType(ListingType.SALE);
        property.setStatus(PropertyStatus.ACTIVE);
        property.setAvailable(true);
        property.setPrice(new BigDecimal("1000000.00"));
        property.setCity("Pune");
        property.setCreatedAt(Instant.parse("2026-10-02T09:00:00Z"));

        lead = new Lead();
        lead.setId(13L);
        lead.setName("Lead, One");
        lead.setSource(LeadSource.REFERRAL);
        lead.setStatus(LeadStatus.QUALIFIED);
        lead.setInterestedProperty(property);
        ReflectionTestUtils.setField(lead, "createdAt", Instant.parse("2026-10-02T10:00:00Z"));

        visit = new SiteVisit();
        visit.setId(17L);
        visit.setLead(lead);
        visit.setProperty(property);
        visit.setScheduledAt(Instant.parse("2026-10-03T10:00:00Z"));
        visit.setStatus(SiteVisitStatus.COMPLETED);
        visit.setNotes("client asked for a follow-up");

        wonDeal = deal(21L, DealStatus.WON, new BigDecimal("1000000.00"), "2026-10-04T10:00:00Z");
        activeDeal = deal(22L, DealStatus.ACTIVE, new BigDecimal("500000.00"), "2026-10-05T10:00:00Z");

        when(leads.searchOwned(7L, null, null, null, null)).thenReturn(List.of(lead));
        when(properties.findByBrokerIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(property));
        when(visits.findOwnedVisits(7L, null)).thenReturn(List.of(visit));
        when(deals.searchOwned(7L, null, null, null)).thenReturn(List.of(wonDeal, activeDeal));
        when(commissionService.summary("broker@example.local")).thenReturn(new CommissionSummaryResponse(
                new BigDecimal("25000.00"),
                new BigDecimal("25000.00"),
                null,
                false,
                List.of(new CommissionRecordResponse(
                        21L, 13L, lead.getName(), 11L, property.getTitle(), "Pune, Pune",
                        new BigDecimal("1000000.00"), new BigDecimal("2.50"),
                        new BigDecimal("25000.00"), DealStatus.WON,
                        Instant.parse("2026-10-04T11:00:00Z")
                ))
        ));
    }

    @Test
    void overviewAggregatesExistingBrokerDataAndUsesWonCommissionsAsPending() {
        ReportOverview report = reportService.overview("broker@example.local", null, null);

        assertEquals(1, report.totalLeads());
        assertEquals(1, report.qualifiedLeads());
        assertEquals(1, report.activeProperties());
        assertEquals(1, report.completedSiteVisits());
        assertEquals(1, report.activeDeals());
        assertEquals(1, report.wonDeals());
        assertEquals(new BigDecimal("1500000.00"), report.totalDealValue());
        assertEquals(0, new BigDecimal("25000.00").compareTo(report.totalCommission()));
        assertEquals(0, new BigDecimal("25000.00").compareTo(report.pendingCommission()));
    }

    @Test
    void leadReportFiltersByStatusAndDateAndListsEnumBuckets() {
        when(leads.searchOwned(7L, null, LeadStatus.QUALIFIED, null, null)).thenReturn(List.of(lead));
        LeadReport report = reportService.leads(
                "broker@example.local", LocalDate.parse("2026-10-02"), LocalDate.parse("2026-10-02"), "qualified"
        );

        assertEquals(1, report.totalLeads());
        assertEquals(1, report.byStatus().get("QUALIFIED"));
        assertEquals(0, report.byStatus().get("NEW"));
        assertEquals(1, report.bySource().get("REFERRAL"));
        verify(leads).searchOwned(7L, null, LeadStatus.QUALIFIED, null, null);
    }

    @Test
    void propertyReportCalculatesValueAndFiltersStatusAndCreatedDate() {
        PropertyReport report = reportService.properties(
                "broker@example.local", LocalDate.parse("2026-10-02"), LocalDate.parse("2026-10-02"), "active"
        );

        assertEquals(1, report.totalProperties());
        assertEquals(new BigDecimal("1000000.00"), report.totalPropertyValue());
        assertEquals(new BigDecimal("1000000.00"), report.averagePropertyValue());
        assertEquals(1, report.byType().get("APARTMENT"));
        assertEquals(1, report.byListingType().get("SALE"));
    }

    @Test
    void siteVisitReportUsesScheduledDateAndRelatedLeadProperty() {
        SiteVisitReport report = reportService.siteVisits(
                "broker@example.local", LocalDate.parse("2026-10-03"), LocalDate.parse("2026-10-03"), null
        );

        assertEquals(1, report.totalVisits());
        assertEquals(1, report.completedVisits());
        assertEquals(1, report.byDate().get("2026-10-03"));
        assertEquals(1, report.byProperty().get(property.getTitle()));
        assertEquals(1, report.byLead().get(lead.getName()));
        assertEquals(lead.getName(), report.visits().getFirst().leadName());
    }

    @Test
    void dealReportCalculatesStagesStatusAndDateFilteredValue() {
        when(deals.searchOwned(7L, null, null, DealStatus.WON)).thenReturn(List.of(wonDeal));
        DealReport report = reportService.deals(
                "broker@example.local", LocalDate.parse("2026-10-04"), LocalDate.parse("2026-10-04"), "won"
        );

        assertEquals(1, report.totalDeals());
        assertEquals(1, report.wonDeals());
        assertEquals(new BigDecimal("1000000.00"), report.totalDealValue());
        assertEquals(new BigDecimal("1000000.00"), report.averageDealValue());
        assertEquals(1, report.byStage().get("CLOSED"));
        assertEquals(1, report.byDate().get("2026-10-04"));
    }

    @Test
    void commissionReportContainsOnlyEarnedWonDealAmountsAndFiltersCommissionDate() {
        CommissionReport report = reportService.commissions(
                "broker@example.local", LocalDate.parse("2026-10-04"), LocalDate.parse("2026-10-04")
        );

        assertEquals(new BigDecimal("25000.00"), report.totalCommission());
        assertEquals(report.totalCommission(), report.pendingCommission());
        assertEquals(new BigDecimal("25000.00"), report.byDate().get("2026-10-04"));
        assertEquals(1, report.commissions().size());
    }

    @Test
    void everyReportLooksUpBrokerAndQueriesRepositoriesByItsId() {
        reportService.leads("broker@example.local", null, null, null);
        reportService.properties("broker@example.local", null, null, null);
        reportService.siteVisits("broker@example.local", null, null, null);
        reportService.deals("broker@example.local", null, null, null);
        reportService.commissions("broker@example.local", null, null);

        verify(leads).searchOwned(7L, null, null, null, null);
        verify(properties).findByBrokerIdOrderByCreatedAtDesc(7L);
        verify(visits).findOwnedVisits(7L, null);
        verify(deals).searchOwned(7L, null, null, null);
    }

    @Test
    void invalidStatusAndNonBrokerAreRejected() {
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> reportService.leads("broker@example.local", null, null, "unrecognized"))
                .getStatusCode().value());
        broker.setRole(Role.BUYER);
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> reportService.overview("broker@example.local", null, null))
                .getStatusCode().value());
    }

    @Test
    void emptyRepositoriesProduceZeroValuedReports() {
        when(leads.searchOwned(7L, null, null, null, null)).thenReturn(List.of());
        when(properties.findByBrokerIdOrderByCreatedAtDesc(7L)).thenReturn(List.of());
        when(visits.findOwnedVisits(7L, null)).thenReturn(List.of());
        when(deals.searchOwned(7L, null, null, null)).thenReturn(List.of());

        assertEquals(0, reportService.leads("broker@example.local", null, null, null).totalLeads());
        assertEquals(BigDecimal.ZERO, reportService.properties("broker@example.local", null, null, null).totalPropertyValue());
        assertEquals(0, reportService.siteVisits("broker@example.local", null, null, null).totalVisits());
        assertEquals(BigDecimal.ZERO, reportService.deals("broker@example.local", null, null, null).totalDealValue());
    }

    private Deal deal(Long id, DealStatus status, BigDecimal value, String createdAt) {
        Deal deal = new Deal();
        deal.setId(id);
        deal.setLead(lead);
        deal.setProperty(property);
        deal.setDealValue(value);
        deal.setCommissionRate(new BigDecimal("2.50"));
        deal.setCommissionAmount(value.multiply(new BigDecimal("0.025")));
        deal.setStage(DealStage.CLOSED);
        deal.setStatus(status);
        ReflectionTestUtils.setField(deal, "createdAt", Instant.parse(createdAt));
        ReflectionTestUtils.setField(deal, "updatedAt", Instant.parse(createdAt));
        return deal;
    }
}
