package com.realestate.deals;

import com.realestate.deals.dto.DealRequest;
import com.realestate.deals.model.Deal;
import com.realestate.deals.model.DealStage;
import com.realestate.deals.model.DealStatus;
import com.realestate.deals.repository.DealRepository;
import com.realestate.leads.model.Lead;
import com.realestate.leads.repository.LeadRepository;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.notifications.NotificationService;
import com.realestate.notifications.model.NotificationType;
import com.realestate.property.model.Property;
import com.realestate.property.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DealServiceTest {

    private final DealRepository deals = org.mockito.Mockito.mock(DealRepository.class);
    private final UserRepository users = org.mockito.Mockito.mock(UserRepository.class);
    private final LeadRepository leads = org.mockito.Mockito.mock(LeadRepository.class);
    private final PropertyRepository properties = org.mockito.Mockito.mock(PropertyRepository.class);
    private final NotificationService notifications = org.mockito.Mockito.mock(NotificationService.class);
    private DealService service;
    private User broker;

    @BeforeEach
    void setUp() {
        service = new DealService(deals, users, leads, properties, notifications);
        broker = new User(14L, "Broker");
        broker.setEmail("broker@example.local");
        broker.setRole(Role.BROKER);
        when(users.findByEmailIgnoreCase("broker@example.local")).thenReturn(Optional.of(broker));
        Lead lead = new Lead();
        lead.setId(5L);
        lead.setName("Buyer");
        Property property = new Property();
        property.setId(8L);
        property.setTitle("Apartment");
        property.setLocation("Main Road");
        property.setCity("Pune");
        when(leads.findOwnedById(5L, 14L)).thenReturn(Optional.of(lead));
        when(properties.findByIdAndBrokerId(8L, 14L)).thenReturn(Optional.of(property));
    }

    @Test
    void createsDealForAuthenticatedBrokerAndCalculatesCommission() {
        when(deals.save(any(Deal.class))).thenAnswer(invocation -> {
            Deal deal = invocation.getArgument(0);
            deal.setId(21L);
            return deal;
        });

        var result = service.create("broker@example.local", request());

        assertEquals(21L, result.id());
        assertEquals(new BigDecimal("25000.00"), result.commissionAmount());
        verify(deals).save(any(Deal.class));
        verify(notifications).createForBroker(
                broker, NotificationType.DEAL, "Deal created", "A deal was created for Buyer", "DEAL", 21L
        );
    }

    @Test
    void cannotAssociateAnotherBrokersLeadOrProperty() {
        when(leads.findOwnedById(99L, 14L)).thenReturn(Optional.empty());
        when(properties.findByIdAndBrokerId(99L, 14L)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(
                ResponseStatusException.class,
                () -> service.create("broker@example.local", new DealRequest(
                        99L, 8L, BigDecimal.TEN, BigDecimal.ONE, DealStage.NEW, DealStatus.ACTIVE, null, null
                ))
        ).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(
                ResponseStatusException.class,
                () -> service.create("broker@example.local", new DealRequest(
                        5L, 99L, BigDecimal.TEN, BigDecimal.ONE, DealStage.NEW, DealStatus.ACTIVE, null, null
                ))
        ).getStatusCode());
        verify(deals, never()).save(any(Deal.class));
    }

    @Test
    void brokerCannotReadOrDeleteAnotherBrokersDeal() {
        when(deals.findOwnedById(91L, 14L)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(
                ResponseStatusException.class,
                () -> service.get("broker@example.local", 91L)
        ).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(
                ResponseStatusException.class,
                () -> service.delete("broker@example.local", 91L)
        ).getStatusCode());
        verify(deals, never()).delete(any(Deal.class));
    }

    private static DealRequest request() {
        return new DealRequest(
                5L,
                8L,
                new BigDecimal("1000000"),
                new BigDecimal("2.5"),
                DealStage.NEGOTIATION,
                DealStatus.ACTIVE,
                null,
                "Offer received"
        );
    }
}
