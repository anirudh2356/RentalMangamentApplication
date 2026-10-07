package com.realestate.team;

import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final TeamService service = new TeamService(users);
    private User broker;
    private User member;

    @BeforeEach
    void setUp() {
        broker = user(20L, "Broker", "broker@example.local", Role.BROKER);
        when(users.findByEmailIgnoreCase("broker@example.local")).thenReturn(Optional.of(broker));
        member = user(34L, "Agent", "agent@example.local", Role.BUYER);
        member.setBroker(broker);
        member.setPasswordHash("must-not-be-returned");
    }

    @Test
    void returnsMembersScopedToAuthenticatedBrokerAndSupportsFilters() {
        when(users.findTeamMembers(20L, Role.BUYER, true, "agent")).thenReturn(List.of(member));

        var result = service.members("broker@example.local", " agent ", Role.BUYER, true);

        assertEquals(1, result.size());
        assertEquals(34L, result.getFirst().id());
        assertEquals("Agent", result.getFirst().name());
        assertEquals(Role.BUYER, result.getFirst().role());
        assertEquals(true, result.getFirst().enabled());
        verify(users).findTeamMembers(20L, Role.BUYER, true, "agent");
    }

    @Test
    void anotherBrokersMembersAreNotQueriedOrReturned() {
        User secondBroker = user(21L, "Second Broker", "second@example.local", Role.BROKER);
        User otherMember = user(35L, "Other Agent", "other@example.local", Role.SELLER);
        otherMember.setBroker(secondBroker);
        when(users.findTeamMembers(20L, null, null, null)).thenReturn(List.of(member));

        var result = service.members("broker@example.local", null, null, null);

        assertEquals(1, result.size());
        assertEquals(34L, result.getFirst().id());
        org.junit.jupiter.api.Assertions.assertFalse(result.stream().anyMatch(item -> item.id().equals(otherMember.getId())));
        verify(users).findTeamMembers(20L, null, null, null);
    }

    @Test
    void rejectsDisabledOrNonBrokerOwner() {
        User buyer = user(22L, "Buyer", "buyer@example.local", Role.BUYER);
        when(users.findByEmailIgnoreCase("buyer@example.local")).thenReturn(Optional.of(buyer));

        assertEquals(HttpStatus.FORBIDDEN, assertThrows(
                ResponseStatusException.class,
                () -> service.members("buyer@example.local", null, null, null)
        ).getStatusCode());
    }

    @Test
    void teamResponseContainsOnlySafeProfileFields() {
        when(users.findTeamMembers(20L, null, null, null)).thenReturn(List.of(member));

        var result = service.members("broker@example.local", null, null, null).getFirst();

        assertEquals("agent@example.local", result.email());
        assertEquals(Instant.parse("2026-10-07T00:00:00Z"), result.createdAt());
        // The response type has no password hash, broker association, or other credential fields.
        assertEquals(
                List.of("id", "name", "email", "role", "enabled", "createdAt"),
                java.util.Arrays.stream(result.getClass().getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName)
                        .toList()
        );
    }

    private static User user(Long id, String name, String email, Role role) {
        User user = new User(id, name);
        user.setEmail(email);
        user.setRole(role);
        user.setCreatedAt(Instant.parse("2026-10-07T00:00:00Z"));
        return user;
    }
}
