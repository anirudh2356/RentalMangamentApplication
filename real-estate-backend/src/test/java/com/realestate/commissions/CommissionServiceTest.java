package com.realestate.commissions;

import com.realestate.deals.model.Deal;
import com.realestate.deals.model.DealStatus;
import com.realestate.deals.repository.DealRepository;
import com.realestate.leads.model.Lead;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.property.model.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class CommissionServiceTest {

    private final DealRepository dealRepository = mock(DealRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private CommissionService service;

    @BeforeEach
    void setUp() {
        service = new CommissionService(dealRepository, userRepository);
    }

    @Test
    void derivesTotalAndPendingCommissionFromAuthenticatedBrokersWonDeals() {
        User broker = user(10L, Role.BROKER);
        when(userRepository.findByEmailIgnoreCase("broker@example.local")).thenReturn(Optional.of(broker));
        when(dealRepository.findOwnedByStatus(10L, DealStatus.WON))
                .thenReturn(List.of(wonDeal(4L, "Alice", "Property A", "12500.00")));

        var summary = service.summary("broker@example.local");

        assertEquals(new BigDecimal("12500.00"), summary.totalCommission());
        assertEquals(new BigDecimal("12500.00"), summary.pendingCommission());
        assertEquals(null, summary.paidCommission());
        assertEquals(false, summary.paymentTrackingSupported());
        assertEquals(1, summary.commissions().size());
        assertEquals("Alice", summary.commissions().getFirst().leadName());
        assertEquals("Property A", summary.commissions().getFirst().propertyTitle());
        verify(dealRepository).findOwnedByStatus(10L, DealStatus.WON);
    }

    @Test
    void rejectsNonBroker() {
        when(userRepository.findByEmailIgnoreCase("buyer@example.local"))
                .thenReturn(Optional.of(user(11L, Role.BUYER)));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.summary("buyer@example.local")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    private static User user(Long id, Role role) {
        User user = new User(id, "User");
        user.setEmail(role == Role.BROKER ? "broker@example.local" : "buyer@example.local");
        user.setRole(role);
        return user;
    }

    private static Deal wonDeal(Long id, String leadName, String propertyTitle, String commission) {
        Lead lead = new Lead();
        lead.setId(20L);
        lead.setName(leadName);
        Property property = new Property();
        property.setId(30L);
        property.setTitle(propertyTitle);
        property.setLocation("Central Road");
        property.setCity("Pune");
        Deal deal = new Deal();
        deal.setId(id);
        deal.setLead(lead);
        deal.setProperty(property);
        deal.setDealValue(new BigDecimal("500000.00"));
        deal.setCommissionRate(new BigDecimal("2.50"));
        deal.setCommissionAmount(new BigDecimal(commission));
        deal.setStatus(DealStatus.WON);
        return deal;
    }
}
