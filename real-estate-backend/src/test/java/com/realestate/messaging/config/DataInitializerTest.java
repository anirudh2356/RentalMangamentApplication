package com.realestate.messaging.config;

import com.realestate.messaging.model.User;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.repository.ConversationRepository;
import com.realestate.messaging.repository.MessageRepository;
import com.realestate.messaging.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataInitializerTest {

    @Test
    void backfillsDemoAccountsWithoutChangingTheirIdsOrPasswords() throws Exception {
        UserRepository users = mock(UserRepository.class);
        ConversationRepository conversations = mock(ConversationRepository.class);
        MessageRepository messages = mock(MessageRepository.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        List<User> existing = List.of(
                new User(1L, "Alice"),
                new User(2L, "Bob"),
                new User(3L, "Carol")
        );
        when(users.count()).thenReturn(3L);
        when(users.findAll()).thenReturn(existing);

        CommandLineRunner initializer = new DataInitializer().seedDemoData(
                users,
                conversations,
                messages,
                encoder,
                "development-password",
                new StandardEnvironment()
        );

        initializer.run();

        assertEquals(List.of(1L, 2L, 3L), existing.stream().map(User::getId).toList());
        assertEquals("alice@example.local", existing.get(0).getEmail());
        assertTrue(encoder.matches("development-password", existing.get(0).getPasswordHash()));
        assertTrue(!existing.get(0).getPasswordHash().equals("development-password"));
        assertTrue(existing.stream().allMatch(user -> user.getRole() != null && user.isEnabled()));
        verify(users).saveAll(existing);
    }

    @Test
    void developmentProfileAssignsBrokerRoleToBobWithoutChangingDemoIds() throws Exception {
        UserRepository users = mock(UserRepository.class);
        ConversationRepository conversations = mock(ConversationRepository.class);
        MessageRepository messages = mock(MessageRepository.class);
        List<User> existing = List.of(
                demoUser(1L, "Alice", "alice@example.local"),
                demoUser(2L, "Bob", "bob@example.local"),
                demoUser(3L, "Carol", "carol@example.local")
        );
        when(users.count()).thenReturn(3L);
        when(users.findAll()).thenReturn(existing);
        StandardEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles("dev");

        CommandLineRunner initializer = new DataInitializer().seedDemoData(
                users,
                conversations,
                messages,
                new BCryptPasswordEncoder(),
                "",
                environment
        );

        initializer.run();

        assertEquals(List.of(1L, 2L, 3L), existing.stream().map(User::getId).toList());
        assertEquals(Role.BUYER, existing.get(0).getRole());
        assertEquals(Role.BROKER, existing.get(1).getRole());
        assertEquals(Role.SELLER, existing.get(2).getRole());
        verify(users).saveAll(existing);
    }

    private User demoUser(Long id, String name, String email) {
        User user = new User(id, name);
        user.setEmail(email);
        user.setRole(Role.BUYER);
        return user;
    }
}
