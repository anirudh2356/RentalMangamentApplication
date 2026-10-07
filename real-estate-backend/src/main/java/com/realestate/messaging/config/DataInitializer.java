package com.realestate.messaging.config;

import com.realestate.messaging.model.Conversation;
import com.realestate.messaging.model.Message;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.ConversationRepository;
import com.realestate.messaging.repository.MessageRepository;
import com.realestate.messaging.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedDemoData(
            UserRepository userRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.auth.demo-password:}") String demoPassword,
            Environment environment
    ) {
        return args -> {
            boolean seedDemoConversations = userRepository.count() == 0;
            Map<Long, String> demoEmails = Map.of(
                    1L, "alice@example.local",
                    2L, "bob@example.local",
                    3L, "carol@example.local"
            );
            Map<Long, Role> developmentRoles = Map.of(
                    1L, Role.BUYER,
                    2L, Role.BROKER,
                    3L, Role.SELLER
            );
            boolean developmentProfile = environment.acceptsProfiles(Profiles.of("dev"));

            if (seedDemoConversations) {
                userRepository.save(new User(1L, "Alice"));
                userRepository.save(new User(2L, "Bob"));
                userRepository.save(new User(3L, "Carol"));
            }

            List<User> existingUsers = userRepository.findAll();
            for (User user : existingUsers) {
                String demoEmail = demoEmails.get(user.getId());
                if (demoEmail != null && (user.getEmail() == null || user.getEmail().isBlank())) {
                    user.setEmail(demoEmail);
                }
                if (user.getRole() == null) {
                    user.setRole(Role.BUYER);
                }
                if (user.getEnabled() == null) {
                    user.setEnabled(true);
                }
                if (user.getCreatedAt() == null) {
                    user.setCreatedAt(Instant.now());
                }
                if (developmentProfile
                        && demoEmail != null
                        && demoEmail.equalsIgnoreCase(user.getEmail())) {
                    user.setRole(developmentRoles.get(user.getId()));
                }
                if (demoEmail != null
                        && demoPassword != null
                        && !demoPassword.isBlank()
                        && (user.getPasswordHash() == null || user.getPasswordHash().isBlank())) {
                    user.setPasswordHash(passwordEncoder.encode(demoPassword));
                }
            }
            userRepository.saveAll(existingUsers);

            if (!seedDemoConversations) {
                return;
            }

            User alice = userRepository.findById(1L).orElseThrow();
            User bob = userRepository.findById(2L).orElseThrow();

            Conversation conversation = new Conversation();
            conversation.setUserOne(alice);
            conversation.setUserTwo(bob);
            conversation.setCreatedAt(Instant.now());
            conversation = conversationRepository.save(conversation);

            Message message = new Message();
            message.setConversation(conversation);
            message.setSender(bob);
            message.setContent("Hi Alice, is the listing still available?");
            message.setSentAt(Instant.now());
            messageRepository.save(message);
        };
    }
}
