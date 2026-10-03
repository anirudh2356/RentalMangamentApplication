package com.realestate.messaging.config;

import com.realestate.messaging.model.Conversation;
import com.realestate.messaging.model.Message;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.ConversationRepository;
import com.realestate.messaging.repository.MessageRepository;
import com.realestate.messaging.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedDemoData(
            UserRepository userRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository
    ) {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }

            User alice = userRepository.save(new User(1L, "Alice"));
            User bob = userRepository.save(new User(2L, "Bob"));
            userRepository.save(new User(3L, "Carol"));

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
