package com.realestate.search;

import com.realestate.documents.repository.DocumentRepository;
import com.realestate.deals.repository.DealRepository;
import com.realestate.followup.repository.FollowUpRepository;
import com.realestate.leads.model.Lead;
import com.realestate.leads.repository.LeadRepository;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.property.repository.PropertyRepository;
import com.realestate.sitevisit.repository.SiteVisitRepository;
import com.realestate.search.dto.SearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final LeadRepository leads = mock(LeadRepository.class);
    private final PropertyRepository properties = mock(PropertyRepository.class);
    private final DealRepository deals = mock(DealRepository.class);
    private final SiteVisitRepository visits = mock(SiteVisitRepository.class);
    private final FollowUpRepository followUps = mock(FollowUpRepository.class);
    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final SearchService searchService =
            new SearchService(users, leads, properties, deals, visits, followUps, documents);

    @BeforeEach
    void setUp() {
        User broker = new User(7L, "Broker");
        broker.setEmail("broker@example.local");
        broker.setRole(Role.BROKER);
        broker.setEnabled(true);
        when(users.findByEmailIgnoreCase("broker@example.local")).thenReturn(Optional.of(broker));
    }

    @Test
    void trimsQueryAndSearchesEveryCategoryForAuthenticatedBroker() {
        Lead lead = new Lead();
        lead.setId(9L);
        lead.setName("Rahul Kumar");
        lead.setEmail("rahul@example.com");
        when(leads.searchOwnedForWorkspace(7L, "RaHuL", PageRequest.of(0, 5)))
                .thenReturn(List.of(lead));

        SearchResponse response = searchService.search("broker@example.local", "  RaHuL  ");

        assertEquals(1, response.results().size());
        assertEquals("LEAD", response.results().getFirst().type());
        assertEquals("Rahul Kumar", response.results().getFirst().title());
        assertEquals("rahul@example.com", response.results().getFirst().subtitle());
        assertEquals("leads", response.results().getFirst().navigationTarget());
        verify(leads).searchOwnedForWorkspace(7L, "RaHuL", PageRequest.of(0, 5));
        verify(properties).searchOwnedForWorkspace(7L, "RaHuL", PageRequest.of(0, 5));
        verify(deals).searchOwnedForWorkspace(7L, "RaHuL", PageRequest.of(0, 5));
        verify(visits).searchOwnedForWorkspace(7L, "RaHuL", PageRequest.of(0, 5));
        verify(followUps).searchOwnedForWorkspace(7L, "RaHuL", PageRequest.of(0, 5));
        verify(documents).searchOwnedForWorkspace(7L, "RaHuL", PageRequest.of(0, 5));
        verify(users).searchTeamMembersForWorkspace(7L, "RaHuL", PageRequest.of(0, 5));
    }

    @Test
    void emptyAndShortQueriesReturnNoResultsWithoutSearching() {
        assertEquals(List.of(), searchService.search("broker@example.local", " ").results());
        assertEquals(List.of(), searchService.search("broker@example.local", "r").results());

        verify(users, never()).findByEmailIgnoreCase("broker@example.local");
        verify(leads, never()).searchOwnedForWorkspace(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsDisabledOrNonBrokerPrincipal() {
        User buyer = new User(8L, "Buyer");
        buyer.setEmail("buyer@example.local");
        buyer.setRole(Role.BUYER);
        buyer.setEnabled(true);
        when(users.findByEmailIgnoreCase("buyer@example.local")).thenReturn(Optional.of(buyer));

        assertThrows(ResponseStatusException.class,
                () -> searchService.search("buyer@example.local", "Rahul"));
        verify(leads, never()).searchOwnedForWorkspace(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }
}
