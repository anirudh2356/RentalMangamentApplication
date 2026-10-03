package com.realestate.messaging;

import com.realestate.messaging.dto.ConversationSummaryDto;
import com.realestate.messaging.dto.MessageDto;
import com.realestate.messaging.model.Conversation;
import com.realestate.messaging.model.Message;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.ConversationRepository;
import com.realestate.messaging.repository.MessageRepository;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.messaging.service.MessagingService;
import com.realestate.messaging.web.ConversationController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MessagingServiceTest {

    private final ConversationRepository conversationRepository =
            mock(ConversationRepository.class);

    private final MessageRepository messageRepository =
            mock(MessageRepository.class);

    private final UserRepository userRepository =
            mock(UserRepository.class);

    private final MessagingService service =
            new MessagingService(
                    conversationRepository,
                    messageRepository,
                    userRepository
            );

    @BeforeEach
    void setUp() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(2L)).thenReturn(true);
        when(userRepository.existsById(3L)).thenReturn(true);
    }

    @Test
    void conversationListIsSortedByLatestMessageAndIncludesUnreadCounts() {
        User alice = new User(1L, "Alice");
        User bob = new User(2L, "Bob");
        User carol = new User(3L, "Carol");

        Conversation bobConversation =
                conversation(10L, alice, bob);

        Conversation carolConversation =
                conversation(11L, alice, carol);

        when(conversationRepository.findAllForUser(1L))
                .thenReturn(List.of(
                        bobConversation,
                        carolConversation
                ));

        when(messageRepository
                .findTopByConversationIdOrderBySentAtDesc(10L))
                .thenReturn(Optional.of(
                        message(
                                20L,
                                bobConversation,
                                bob,
                                "newest",
                                "2026-09-30T10:00:00Z"
                        )
                ));

        when(messageRepository
                .findTopByConversationIdOrderBySentAtDesc(11L))
                .thenReturn(Optional.of(
                        message(
                                21L,
                                carolConversation,
                                carol,
                                "older",
                                "2026-09-30T09:00:00Z"
                        )
                ));

        when(messageRepository
                .countByConversationIdAndSenderIdNotAndReadFalse(10L, 1L))
                .thenReturn(2L);

        when(messageRepository
                .countByConversationIdAndSenderIdNotAndReadFalse(11L, 1L))
                .thenReturn(0L);

        List<ConversationSummaryDto> result =
                service.listConversations(1L);

        assertEquals(10L, result.get(0).id());
        assertEquals(
                "Bob",
                result.get(0).otherUserName()
        );
        assertEquals(
                "newest",
                result.get(0).latestMessage()
        );
        assertEquals(
                2L,
                result.get(0).unreadCount()
        );

        assertEquals(11L, result.get(1).id());
        assertEquals(
                0L,
                result.get(1).unreadCount()
        );
    }

    @Test
    void createConversationReusesExistingPair() {
        User alice = new User(1L, "Alice");
        User bob = new User(2L, "Bob");

        Conversation existing =
                conversation(12L, alice, bob);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(alice));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(bob));

        when(conversationRepository.findBetweenUsers(1L, 2L))
                .thenReturn(Optional.of(existing));

        when(messageRepository
                .findTopByConversationIdOrderBySentAtDesc(12L))
                .thenReturn(Optional.empty());

        when(messageRepository
                .countByConversationIdAndSenderIdNotAndReadFalse(12L, 1L))
                .thenReturn(0L);

        ConversationSummaryDto result =
                service.createConversation(1L, 2L);

        assertEquals(12L, result.id());
        assertEquals(
                "Bob",
                result.otherUserName()
        );

        verify(
                conversationRepository,
                never()
        ).save(any(Conversation.class));
    }

    @Test
    void messageHistoryIsMappedInRepositoryChronologicalOrder() {
        User alice = new User(1L, "Alice");
        User bob = new User(2L, "Bob");

        Conversation conversation =
                conversation(13L, alice, bob);

        Message first =
                message(
                        30L,
                        conversation,
                        bob,
                        "first",
                        "2026-09-30T08:00:00Z"
                );

        Message second =
                message(
                        31L,
                        conversation,
                        alice,
                        "second",
                        "2026-09-30T08:01:00Z"
                );

        when(conversationRepository.findByIdForUser(13L, 1L))
                .thenReturn(Optional.of(conversation));

        when(messageRepository
                .findByConversationIdOrderBySentAtAsc(13L))
                .thenReturn(List.of(first, second));

        List<MessageDto> result =
                service.getMessages(13L, 1L);

        assertEquals(
                List.of("first", "second"),
                result.stream()
                        .map(MessageDto::content)
                        .toList()
        );

        assertEquals(
                "Bob",
                result.get(0).senderName()
        );
    }

    @Test
    void sendMessageTrimsAndPersistsOneMessage() {
        User alice = new User(1L, "Alice");
        User bob = new User(2L, "Bob");

        Conversation conversation =
                conversation(14L, alice, bob);

        when(conversationRepository.findByIdForUser(14L, 1L))
                .thenReturn(Optional.of(conversation));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(alice));

        when(messageRepository.save(any(Message.class)))
                .thenAnswer(invocation -> {
                    Message saved =
                            invocation.getArgument(0);

                    saved.setId(32L);

                    return saved;
                });

        MessageDto result =
                service.sendMessage(
                        14L,
                        "  hello  ",
                        1L
                );

        assertEquals(32L, result.id());
        assertEquals(1L, result.senderId());
        assertEquals("hello", result.content());

        verify(messageRepository)
                .save(any(Message.class));
    }

    @Test
    void emptyAndOversizedMessagesAreRejected() {
        ResponseStatusException empty =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.sendMessage(
                                14L,
                                "  ",
                                1L
                        )
                );

        ResponseStatusException oversized =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.sendMessage(
                                14L,
                                "x".repeat(2001),
                                1L
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                empty.getStatusCode()
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                oversized.getStatusCode()
        );

        verify(
                conversationRepository,
                never()
        ).findByIdForUser(
                eq(14L),
                eq(1L)
        );
    }

    @Test
    void markingReadOnlyUpdatesIncomingMessagesAndExposesNoContentRoute()
            throws Exception {

        User alice = new User(1L, "Alice");
        User bob = new User(2L, "Bob");

        Conversation conversation =
                conversation(15L, alice, bob);

        when(conversationRepository.findByIdForUser(15L, 1L))
                .thenReturn(Optional.of(conversation));

        when(messageRepository.markMessagesAsRead(15L, 1L))
                .thenReturn(1);

        service.markConversationAsRead(15L, 1L);

        verify(messageRepository)
                .markMessagesAsRead(15L, 1L);

        MockMvc mvc =
                MockMvcBuilders
                        .standaloneSetup(
                                new ConversationController(service)
                        )
                        .build();

        mvc.perform(
                post("/api/conversations/15/read")
                        .header("X-User-Id", "1")
        ).andExpect(
                status().isNoContent()
        );
    }

    @Test
    void cannotReadConversationOutsideCurrentUsersMembership() {
        when(conversationRepository.findByIdForUser(99L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.markConversationAsRead(
                                99L,
                                1L
                        )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(
                messageRepository,
                never()
        ).markMessagesAsRead(99L, 1L);
    }

    private Conversation conversation(
            long id,
            User userOne,
            User userTwo
    ) {
        Conversation conversation =
                new Conversation();

        conversation.setId(id);
        conversation.setUserOne(userOne);
        conversation.setUserTwo(userTwo);
        conversation.setCreatedAt(
                Instant.parse(
                        "2026-09-30T07:00:00Z"
                )
        );

        return conversation;
    }

    private Message message(
            long id,
            Conversation conversation,
            User sender,
            String content,
            String sentAt
    ) {
        Message message =
                new Message();

        message.setId(id);
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content);
        message.setSentAt(
                Instant.parse(sentAt)
        );

        return message;
    }

    private static <T> T mock(Class<T> type) {
        return org.mockito.Mockito.mock(type);
    }
}