package com.realestate.messaging.repository;

import com.realestate.messaging.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderBySentAtAsc(
            Long conversationId
    );

    Optional<Message> findTopByConversationIdOrderBySentAtDesc(
            Long conversationId
    );

    long countByConversationIdAndSenderIdNotAndReadFalse(
            Long conversationId,
            Long senderId
    );

    // Used for editing/deleting only the current user's own message
    Optional<Message> findByIdAndConversation_IdAndSender_Id(
            Long messageId,
            Long conversationId,
            Long senderId
    );

    @Modifying
    @Query("""
            update Message m
            set m.read = true
            where m.conversation.id = :conversationId
              and m.sender.id <> :currentUserId
              and m.read = false
            """)
    int markMessagesAsRead(
            @Param("conversationId") Long conversationId,
            @Param("currentUserId") Long currentUserId
    );
}