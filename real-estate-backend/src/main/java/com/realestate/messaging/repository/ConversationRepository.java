package com.realestate.messaging.repository;

import com.realestate.messaging.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("""
            SELECT c FROM Conversation c
            WHERE c.userOne.id = :userId OR c.userTwo.id = :userId
            ORDER BY c.id DESC
            """)
    List<Conversation> findAllForUser(@Param("userId") Long userId);

    @Query("""
            SELECT c FROM Conversation c
            WHERE c.id = :id AND (c.userOne.id = :userId OR c.userTwo.id = :userId)
            """)
    Optional<Conversation> findByIdForUser(@Param("id") Long id, @Param("userId") Long userId);

    @Query("""
            SELECT c FROM Conversation c
            WHERE (c.userOne.id = :a AND c.userTwo.id = :b)
               OR (c.userOne.id = :b AND c.userTwo.id = :a)
            """)
    Optional<Conversation> findBetweenUsers(@Param("a") Long userOneId, @Param("b") Long userTwoId);
}
