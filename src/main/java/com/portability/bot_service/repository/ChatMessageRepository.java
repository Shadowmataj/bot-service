package com.portability.bot_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.portability.bot_service.model.entity.ChatMessage;

/**
 * Repository for managing chat messages in PostgreSQL
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByMessageOrderAsc(String conversationId);

    @Query("SELECT MAX(m.messageOrder) FROM ChatMessage m WHERE m.conversationId = :conversationId")
    Integer findMaxMessageOrderByConversationId(@Param("conversationId") String conversationId);

    @Query("SELECT m FROM ChatMessage m WHERE m.conversationId = :conversationId ORDER BY m.messageOrder DESC")
    List<ChatMessage> findRecentMessagesByConversationId(
            @Param("conversationId") String conversationId,
            org.springframework.data.domain.Pageable pageable
    );

    void deleteByConversationId(String conversationId);

    long countByConversationId(String conversationId);
}
