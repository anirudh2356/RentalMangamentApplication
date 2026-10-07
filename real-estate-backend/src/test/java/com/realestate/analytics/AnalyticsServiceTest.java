package com.realestate.analytics;

import com.realestate.deals.model.DealStage;
import com.realestate.deals.model.DealStatus;
import com.realestate.deals.repository.DealRepository;
import com.realestate.leads.model.LeadStatus;
import com.realestate.leads.repository.LeadRepository;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.property.model.PropertyStatus;
import com.realestate.property.repository.PropertyRepository;
import com.realestate.sitevisit.model.SiteVisitStatus;
import com.realestate.sitevisit.repository.SiteVisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyticsServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final LeadRepository leads = mock(LeadRepository.class);
    private final PropertyRepository properties = mock(PropertyRepository.class);
    private final SiteVisitRepository visits = mock(SiteVisitRepository.class);
    private final DealRepository deals = mock(DealRepository.class);
    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsService(users, leads, properties, visits, deals);
    }

    @Test
    void aggregatesOnlyAuthenticatedBrokersRecords() {
        User broker = broker(24L);
        when(users.findByEmailIgnoreCase("broker@example.local")).thenReturn(Optional.of(broker));
        when(leads.countByBrokerId(24L)).thenReturn(7L);
        when(leads.countByBrokerIdAndStatus(24L, LeadStatus.NEW)).thenReturn(3L);
        when(leads.countByBrokerIdAndStatus(24L, LeadStatus.QUALIFIED)).thenReturn(2L);
        when(leads.countByBrokerIdAndStatus(24L, LeadStatus.WON)).thenReturn(1L);
        when(leads.countByBrokerIdAndStatus(24L, LeadStatus.LOST)).thenReturn(1L);
        when(properties.countByBrokerId(24L)).thenReturn(5L);
        when(properties.countByBrokerIdAndStatus(24L, PropertyStatus.ACTIVE)).thenReturn(3L);
        when(properties.countByBrokerIdAndStatus(24L, PropertyStatus.SOLD)).thenReturn(1L);
        when(properties.countByBrokerIdAndStatus(24L, PropertyStatus.RENTED)).thenReturn(1L);
        when(visits.countUpcoming(24L, List.of(SiteVisitStatus.SCHEDULED, SiteVisitStatus.RESCHEDULED))).thenReturn(2L);
        when(visits.countByBrokerIdAndStatus(24L, SiteVisitStatus.COMPLETED)).thenReturn(4L);
        when(deals.countByBrokerIdAndStatus(24L, DealStatus.ACTIVE)).thenReturn(2L);
        when(deals.countByBrokerIdAndStatus(24L, DealStatus.WON)).thenReturn(1L);
        when(deals.countByBrokerIdAndStatus(24L, DealStatus.LOST)).thenReturn(1L);
        when(deals.countByBrokerIdAndStage(24L, DealStage.NEGOTIATION)).thenReturn(2L);
        when(deals.sumDealValueByBrokerId(24L)).thenReturn(new BigDecimal("1500000.00"));
        when(deals.sumCommissionByBrokerIdAndStatus(24L, DealStatus.WON)).thenReturn(new BigDecimal("30000.00"));

        var result = service.overview("broker@example.local");

        assertEquals(7L, result.totalLeads());
        assertEquals(3L, result.newLeads());
        assertEquals(2L, result.qualifiedLeads());
        assertEquals(1L, result.wonLeads());
        assertEquals(1L, result.lostLeads());
        assertEquals(2L, result.leadPipeline().get("QUALIFIED"));
        assertEquals(5L, result.totalProperties());
        assertEquals(3L, result.activeProperties());
        assertEquals(2L, result.soldRentedProperties());
        assertEquals(2L, result.upcomingSiteVisits());
        assertEquals(4L, result.completedSiteVisits());
        assertEquals(2L, result.activeDeals());
        assertEquals(1L, result.wonDeals());
        assertEquals(1L, result.lostDeals());
        assertEquals(2L, result.dealPipeline().get("NEGOTIATION"));
        assertEquals(new BigDecimal("1500000.00"), result.totalDealValue());
        assertEquals(new BigDecimal("30000.00"), result.totalCommission());
        assertEquals(new BigDecimal("30000.00"), result.pendingCommission());
        assertEquals(false, result.paymentTrackingSupported());
        verify(deals).sumDealValueByBrokerId(24L);
    }

    @Test
    void resolvesAndScopesAnalyticsToAuthenticatedBrokerWithNoRecords() {
        when(users.findByEmailIgnoreCase("second@example.local")).thenReturn(Optional.of(broker(99L)));
        when(deals.sumDealValueByBrokerId(99L)).thenReturn(BigDecimal.ZERO);
        when(deals.sumCommissionByBrokerIdAndStatus(99L, DealStatus.WON)).thenReturn(BigDecimal.ZERO);

        var result = service.overview("second@example.local");

        assertEquals(0L, result.totalLeads());
        assertEquals(0L, result.totalProperties());
        assertEquals(0L, result.upcomingSiteVisits());
        assertEquals(0L, result.activeDeals());
        assertEquals(BigDecimal.ZERO, result.totalDealValue());
        assertEquals(BigDecimal.ZERO, result.totalCommission());
        verify(leads).countByBrokerId(99L);
        verify(properties).countByBrokerId(99L);
        verify(visits).countUpcoming(99L, List.of(SiteVisitStatus.SCHEDULED, SiteVisitStatus.RESCHEDULED));
        verify(deals).sumDealValueByBrokerId(99L);
    }

    @Test
    void rejectsNonBrokerAnalyticsAccess() {
        User buyer = broker(12L);
        buyer.setRole(Role.BUYER);
        when(users.findByEmailIgnoreCase("buyer@example.local")).thenReturn(Optional.of(buyer));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.overview("buyer@example.local")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    private static User broker(Long id) {
        User user = new User(id, "Broker");
        user.setEmail(id == 12L ? "buyer@example.local" : "broker@example.local");
        user.setRole(Role.BROKER);
        return user;
    }
}
