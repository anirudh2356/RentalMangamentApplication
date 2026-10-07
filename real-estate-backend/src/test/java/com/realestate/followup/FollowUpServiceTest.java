package com.realestate.followup;

import com.realestate.followup.dto.FollowUpRequest;
import com.realestate.followup.model.FollowUp;
import com.realestate.followup.model.FollowUpPriority;
import com.realestate.followup.model.FollowUpStatus;
import com.realestate.followup.repository.FollowUpRepository;
import com.realestate.leads.model.Lead;
import com.realestate.leads.repository.LeadRepository;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.notifications.NotificationService;
import com.realestate.notifications.model.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FollowUpServiceTest {

    private final FollowUpRepository followUps = mock(FollowUpRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final LeadRepository leads = mock(LeadRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final FollowUpService service = new FollowUpService(followUps, users, leads, notifications);
    private User broker;
    private Lead lead;

    @BeforeEach
    void setUp() {
        broker = new User(14L, "Broker");
        broker.setEmail("broker@example.local");
        broker.setRole(Role.BROKER);
        when(users.findByEmailIgnoreCase("broker@example.local")).thenReturn(Optional.of(broker));

        lead = new Lead();
        lead.setId(5L);
        lead.setName("Buyer");
        when(leads.findOwnedById(5L, 14L)).thenReturn(Optional.of(lead));
    }

    @Test
    void createsFollowUpOwnedByAuthenticatedBrokerAndOwnedLead() {
        when(followUps.save(any(FollowUp.class))).thenAnswer(invocation -> {
            FollowUp followUp = invocation.getArgument(0);
            followUp.setId(31L);
            return followUp;
        });

        var response = service.create("broker@example.local", request());

        assertEquals(31L, response.id());
        assertEquals(5L, response.leadId());
        assertEquals("Buyer", response.leadName());
        verify(followUps).save(org.mockito.ArgumentMatchers.argThat(followUp ->
                followUp.getBroker() == broker && followUp.getLead() == lead));
        verify(notifications).createForBroker(
                broker,
                NotificationType.FOLLOW_UP,
                "Follow-up scheduled",
                "A follow-up was scheduled for Buyer",
                "FOLLOW_UP",
                31L
        );
        verify(leads).findOwnedById(5L, 14L);
    }

    @Test
    void listsAndRetrievesOnlyOwnedFollowUps() {
        FollowUp followUp = followUp(31L);
        when(followUps.findOwned(14L, FollowUpStatus.PENDING, FollowUpPriority.HIGH, 5L,
                null, null, "call")).thenReturn(List.of(followUp));
        when(followUps.findOwnedById(31L, 14L)).thenReturn(Optional.of(followUp));

        var results = service.list("broker@example.local", FollowUpStatus.PENDING,
                FollowUpPriority.HIGH, 5L, null, null, " call ");
        var result = service.get("broker@example.local", 31L);

        assertEquals(1, results.size());
        assertEquals("Call buyer", result.title());
        verify(followUps).findOwned(14L, FollowUpStatus.PENDING, FollowUpPriority.HIGH, 5L,
                null, null, "call");
    }

    @Test
    void brokerCannotGetUpdateOrDeleteAnotherBrokersFollowUp() {
        when(followUps.findOwnedById(88L, 14L)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(
                ResponseStatusException.class,
                () -> service.get("broker@example.local", 88L)
        ).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(
                ResponseStatusException.class,
                () -> service.update("broker@example.local", 88L, request())
        ).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(
                ResponseStatusException.class,
                () -> service.delete("broker@example.local", 88L)
        ).getStatusCode());
        verify(followUps, never()).save(any(FollowUp.class));
        verify(followUps, never()).delete(any(FollowUp.class));
    }

    @Test
    void rejectsLeadNotOwnedByAuthenticatedBroker() {
        when(leads.findOwnedById(99L, 14L)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(
                ResponseStatusException.class,
                () -> service.create("broker@example.local", new FollowUpRequest(
                        "Call buyer", null, 99L, Instant.parse("2026-10-08T12:30:00Z"),
                        FollowUpStatus.PENDING, FollowUpPriority.HIGH
                ))
        ).getStatusCode());
        verify(followUps, never()).save(any(FollowUp.class));
    }

    @Test
    void rejectsInvalidDateRangeAndNonBroker() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(
                ResponseStatusException.class,
                () -> service.list("broker@example.local", null, null, null,
                        Instant.parse("2026-10-10T00:00:00Z"), Instant.parse("2026-10-09T00:00:00Z"), null)
        ).getStatusCode());

        User buyer = new User(15L, "Buyer");
        buyer.setEmail("buyer@example.local");
        buyer.setRole(Role.BUYER);
        when(users.findByEmailIgnoreCase("buyer@example.local")).thenReturn(Optional.of(buyer));
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(
                ResponseStatusException.class,
                () -> service.list("buyer@example.local", null, null, null, null, null, null)
        ).getStatusCode());
    }

    private static FollowUpRequest request() {
        return new FollowUpRequest(
                "Call buyer",
                "Discuss preferred listings",
                5L,
                Instant.parse("2026-10-08T12:30:00Z"),
                FollowUpStatus.PENDING,
                FollowUpPriority.HIGH
        );
    }

    private FollowUp followUp(Long id) {
        FollowUp followUp = new FollowUp();
        followUp.setId(id);
        followUp.setBroker(broker);
        followUp.setLead(lead);
        followUp.setTitle("Call buyer");
        followUp.setFollowUpAt(Instant.parse("2026-10-08T12:30:00Z"));
        followUp.setStatus(FollowUpStatus.PENDING);
        followUp.setPriority(FollowUpPriority.HIGH);
        return followUp;
    }
}
