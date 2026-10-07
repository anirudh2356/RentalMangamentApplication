package com.realestate.calendar;

import com.realestate.followup.model.FollowUp;
import com.realestate.followup.model.FollowUpPriority;
import com.realestate.followup.model.FollowUpStatus;
import com.realestate.followup.repository.FollowUpRepository;
import com.realestate.leads.model.Lead;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.property.model.Property;
import com.realestate.sitevisit.model.SiteVisit;
import com.realestate.sitevisit.model.SiteVisitStatus;
import com.realestate.sitevisit.repository.SiteVisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CalendarServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final SiteVisitRepository visits = mock(SiteVisitRepository.class);
    private final FollowUpRepository followUps = mock(FollowUpRepository.class);
    private final CalendarService service = new CalendarService(users, visits, followUps);
    private final User broker = new User(14L, "Broker");

    @BeforeEach
    void setUp() {
        broker.setEmail("broker@example.local");
        broker.setRole(Role.BROKER);
        when(users.findByEmailIgnoreCase("broker@example.local")).thenReturn(Optional.of(broker));
    }

    @Test
    void mapsOwnedSiteVisitsAndFollowUpsIntoSortedCalendarEvents() {
        when(visits.findCalendarVisits(14L, null, null)).thenReturn(List.of(siteVisit(7L)));
        when(followUps.findCalendarFollowUps(14L, null, null)).thenReturn(List.of(followUp(9L)));

        var events = service.events("broker@example.local", null, null);

        assertEquals(2, events.size());
        assertEquals("FOLLOW_UP-9", events.get(0).id());
        assertEquals("FOLLOW_UP", events.get(0).type().name());
        assertEquals(LocalDate.parse("2026-10-08"), events.get(0).date());
        assertEquals("Call buyer", events.get(0).title());
        assertEquals(5L, events.get(0).relatedLeadId());
        assertEquals("SITE_VISIT-7", events.get(1).id());
        assertEquals("SITE_VISIT", events.get(1).type().name());
        assertEquals(30L, events.get(1).relatedPropertyId());
        assertEquals("Apartment", events.get(1).relatedPropertyTitle());
    }

    @Test
    void appliesInclusiveDateRangeAndQueriesOnlyAuthenticatedBrokerRecords() {
        LocalDate from = LocalDate.parse("2026-10-01");
        LocalDate to = LocalDate.parse("2026-10-31");
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toExclusive = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        when(visits.findCalendarVisits(14L, fromInstant, toExclusive)).thenReturn(List.of(siteVisit(7L)));
        when(followUps.findCalendarFollowUps(14L, fromInstant, toExclusive)).thenReturn(List.of(followUp(9L)));

        service.events("broker@example.local", from, to);

        verify(visits).findCalendarVisits(14L, fromInstant, toExclusive);
        verify(followUps).findCalendarFollowUps(14L, fromInstant, toExclusive);
    }

    @Test
    void doesNotReturnAnotherBrokersAppointmentsOrFollowUps() {
        when(visits.findCalendarVisits(14L, null, null)).thenReturn(List.of());
        when(followUps.findCalendarFollowUps(14L, null, null)).thenReturn(List.of());

        assertEquals(List.of(), service.events("broker@example.local", null, null));
        verify(visits).findCalendarVisits(14L, null, null);
        verify(followUps).findCalendarFollowUps(14L, null, null);
    }

    @Test
    void returnsEmptyEventsAndRejectsInvalidRangeOrNonBroker() {
        when(visits.findCalendarVisits(14L, null, null)).thenReturn(List.of());
        when(followUps.findCalendarFollowUps(14L, null, null)).thenReturn(List.of());
        assertEquals(List.of(), service.events("broker@example.local", null, null));

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(
                ResponseStatusException.class,
                () -> service.events("broker@example.local",
                        LocalDate.parse("2026-10-10"), LocalDate.parse("2026-10-01"))
        ).getStatusCode());

        User buyer = new User(15L, "Buyer");
        buyer.setEmail("buyer@example.local");
        buyer.setRole(Role.BUYER);
        when(users.findByEmailIgnoreCase("buyer@example.local")).thenReturn(Optional.of(buyer));
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(
                ResponseStatusException.class,
                () -> service.events("buyer@example.local", null, null)
        ).getStatusCode());
    }

    private static SiteVisit siteVisit(Long id) {
        Lead lead = new Lead();
        lead.setId(5L);
        lead.setName("Buyer");
        Property property = new Property();
        property.setId(30L);
        property.setTitle("Apartment");
        property.setLocation("Main Road");
        property.setCity("Pune");
        SiteVisit visit = new SiteVisit();
        visit.setId(id);
        visit.setLead(lead);
        visit.setProperty(property);
        visit.setScheduledAt(Instant.parse("2026-10-08T11:00:00Z"));
        visit.setStatus(SiteVisitStatus.SCHEDULED);
        visit.setNotes("Bring documents");
        return visit;
    }

    private static FollowUp followUp(Long id) {
        Lead lead = new Lead();
        lead.setId(5L);
        lead.setName("Buyer");
        FollowUp followUp = new FollowUp();
        followUp.setId(id);
        followUp.setLead(lead);
        followUp.setTitle("Call buyer");
        followUp.setFollowUpAt(Instant.parse("2026-10-08T09:00:00Z"));
        followUp.setStatus(FollowUpStatus.PENDING);
        followUp.setPriority(FollowUpPriority.HIGH);
        followUp.setDescription("Check interest");
        return followUp;
    }
}
