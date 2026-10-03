package com.realestate.messaging.service;

import com.realestate.messaging.dto.ConversationSummaryDto;
import com.realestate.messaging.dto.MessageDto;
import com.realestate.messaging.model.Conversation;
import com.realestate.messaging.model.Message;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.ConversationRepository;
import com.realestate.messaging.repository.MessageRepository;
import com.realestate.messaging.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class MessagingService {

    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessagingService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            UserRepository userRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryDto> listConversations(
            Long currentUserId
    ) {
        requireUser(currentUserId);

        List<Conversation> conversations =
                conversationRepository.findAllForUser(currentUserId);

        List<ConversationSummaryDto> result = new ArrayList<>();

        for (Conversation conversation : conversations) {
            User other = otherUser(
                    conversation,
                    currentUserId
            );

            Optional<Message> latest =
                    messageRepository
                            .findTopByConversationIdOrderBySentAtDesc(
                                    conversation.getId()
                            );

            long unreadCount =
                    messageRepository
                            .countByConversationIdAndSenderIdNotAndReadFalse(
                                    conversation.getId(),
                                    currentUserId
                            );

            result.add(
                    new ConversationSummaryDto(
                            conversation.getId(),
                            other.getId(),
                            other.getName(),
                            latest.map(Message::getContent)
                                    .orElse(""),
                            latest.map(Message::getSentAt)
                                    .orElse(
                                            conversation.getCreatedAt()
                                    ),
                            unreadCount
                    )
            );
        }

        result.sort(
                Comparator.comparing(
                        ConversationSummaryDto::latestMessageAt
                ).reversed()
        );

        return result;
    }

    @Transactional
    public ConversationSummaryDto createConversation(
            Long currentUserId,
            Long otherUserId
    ) {
        if (
                currentUserId == null ||
                otherUserId == null ||
                otherUserId.equals(currentUserId)
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid users"
            );
        }

        User current =
                userRepository.findById(currentUserId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Current user not found"
                                )
                        );

        User other =
                userRepository.findById(otherUserId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "User not found"
                                )
                        );

        Optional<Conversation> existing =
                conversationRepository.findBetweenUsers(
                        currentUserId,
                        otherUserId
                );

        if (existing.isPresent()) {
            Conversation conversation = existing.get();

            return toSummary(
                    conversation,
                    other,
                    currentUserId
            );
        }

        Conversation conversation =
                new Conversation();

        conversation.setUserOne(current);
        conversation.setUserTwo(other);
        conversation.setCreatedAt(Instant.now());

        conversation =
                conversationRepository.save(conversation);

        return new ConversationSummaryDto(
                conversation.getId(),
                other.getId(),
                other.getName(),
                "",
                conversation.getCreatedAt(),
                0
        );
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getMessages(
            Long conversationId,
            Long currentUserId
    ) {
        requireUser(currentUserId);

        Conversation conversation =
                conversationRepository
                        .findByIdForUser(
                                conversationId,
                                currentUserId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Conversation not found"
                                )
                        );

        return messageRepository
                .findByConversationIdOrderBySentAtAsc(
                        conversation.getId()
                )
                .stream()
                .map(this::toMessageDto)
                .toList();
    }

    @Transactional
    public void markConversationAsRead(
            Long conversationId,
            Long currentUserId
    ) {
        requireUser(currentUserId);

        Conversation conversation =
                conversationRepository
                        .findByIdForUser(
                                conversationId,
                                currentUserId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Conversation not found"
                                )
                        );

        messageRepository.markMessagesAsRead(
                conversation.getId(),
                currentUserId
        );
    }

    @Transactional
    public MessageDto sendMessage(
            Long conversationId,
            String content,
            Long currentUserId
    ) {
        requireUser(currentUserId);

        validateMessageContent(content);

        Conversation conversation =
                conversationRepository
                        .findByIdForUser(
                                conversationId,
                                currentUserId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Conversation not found"
                                )
                        );

        User sender =
                userRepository.findById(currentUserId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Current user not found"
                                )
                        );

        Message message =
                new Message();

        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content.trim());
        message.setSentAt(Instant.now());
        message.setRead(false);

        message =
                messageRepository.save(message);

        return toMessageDto(message);
    }

    @Transactional
    public MessageDto editMessage(
            Long conversationId,
            Long messageId,
            String content,
            Long currentUserId
    ) {
        requireUser(currentUserId);

        validateMessageContent(content);

        // This guarantees:
        // 1. message belongs to this conversation
        // 2. message belongs to the current user
        Message message =
                messageRepository
                        .findByIdAndConversation_IdAndSender_Id(
                                messageId,
                                conversationId,
                                currentUserId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Message not found or you are not the sender"
                                )
                        );

        message.setContent(content.trim());

        message =
                messageRepository.save(message);

        return toMessageDto(message);
    }

    @Transactional
    public void deleteMessage(
            Long conversationId,
            Long messageId,
            Long currentUserId
    ) {
        requireUser(currentUserId);

        Message message =
                messageRepository
                        .findByIdAndConversation_IdAndSender_Id(
                                messageId,
                                conversationId,
                                currentUserId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Message not found or you are not the sender"
                                )
                        );

        messageRepository.delete(message);
    }

    private void validateMessageContent(
            String content
    ) {
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Message content is required"
            );
        }

        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Message must be 2000 characters or fewer"
            );
        }
    }

    private User otherUser(
            Conversation conversation,
            Long currentUserId
    ) {
        if (
                conversation
                        .getUserOne()
                        .getId()
                        .equals(currentUserId)
        ) {
            return conversation.getUserTwo();
        }

        return conversation.getUserOne();
    }

    private ConversationSummaryDto toSummary(
            Conversation conversation,
            User other,
            Long currentUserId
    ) {
        Optional<Message> latest =
                messageRepository
                        .findTopByConversationIdOrderBySentAtDesc(
                                conversation.getId()
                        );

        long unreadCount =
                messageRepository
                        .countByConversationIdAndSenderIdNotAndReadFalse(
                                conversation.getId(),
                                currentUserId
                        );

        return new ConversationSummaryDto(
                conversation.getId(),
                other.getId(),
                other.getName(),
                latest.map(Message::getContent)
                        .orElse(""),
                latest.map(Message::getSentAt)
                        .orElse(conversation.getCreatedAt()),
                unreadCount
        );
    }

    private MessageDto toMessageDto(
            Message message
    ) {
        User sender = message.getSender();

        return new MessageDto(
                message.getId(),
                sender.getId(),
                sender.getName(),
                message.getContent(),
                message.getSentAt()
        );
    }

    private void requireUser(
            Long userId
    ) {
        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User ID is required"
            );
        }

        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Current user not found"
            );
        }
    }
}