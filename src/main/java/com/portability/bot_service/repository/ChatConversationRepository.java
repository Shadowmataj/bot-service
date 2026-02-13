package com.portability.bot_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.portability.bot_service.model.entity.ChatConversation;

/**
 * Repository for managing chat conversations in PostgreSQL
 */
@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    Optional<ChatConversation> findByConversationId(String conversationId);

    Optional<ChatConversation> findByPhoneNumber(String phoneNumber);

    boolean existsByConversationId(String conversationId);

    /**
     * Find conversations older than a specific date that are still active. Used
     * by cleanup service to identify conversations eligible for data cleanup.
     */
    List<ChatConversation> findByUpdatedAtBeforeAndIsActiveTrue(LocalDateTime date);
}
