package com.portability.bot_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portability.bot_service.model.entity.ChatConversation;
import com.portability.bot_service.repository.ChatConversationRepository;

/**
 * Service for automatically cleaning up sensitive data from old conversations.
 * 
 * GDPR/LGPD Compliance: Personal data should not be kept longer than necessary.
 * This service removes sensitive PII after a retention period (default: 30 days).
 * 
 * Sensitive data removed:
 * - customer_email, customer_phone
 * - address details (street, district, reference)
 * - portability_nip, portability_imei
 * - checkout_session_url
 * 
 * Data kept for analytics/reference:
 * - IDs (customer_id, order_id, etc.)
 * - conversation state
 * - timestamps
 */
@Service
public class ContextDataCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(ContextDataCleanupService.class);
    
    private static final int RETENTION_DAYS = 30;
    
    private final ChatConversationRepository conversationRepository;
    
    public ContextDataCleanupService(ChatConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }
    
    /**
     * Scheduled task to clean up sensitive data from old conversations.
     * Runs daily at 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupSensitiveData() {
        logger.info("Starting scheduled sensitive data cleanup task...");
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(RETENTION_DAYS);
        
        // Find conversations older than retention period
        List<ChatConversation> oldConversations = conversationRepository
            .findByUpdatedAtBeforeAndIsActiveTrue(cutoffDate);
        
        int cleanedCount = 0;
        for (ChatConversation conversation : oldConversations) {
            try {
                if (removeSensitiveFields(conversation)) {
                    cleanedCount++;
                }
            } catch (Exception e) {
                logger.error("Failed to clean conversation {}", conversation.getConversationId(), e);
            }
        }
        
        logger.info("Sensitive data cleanup completed. Cleaned {} out of {} old conversations", 
                   cleanedCount, oldConversations.size());
    }
    
    /**
     * Remove sensitive fields from a conversation's context data
     * 
     * @return true if data was cleaned, false if already cleaned
     */
    private boolean removeSensitiveFields(ChatConversation conversation) {
        Map<String, Object> context = conversation.getContextData();
        
        if (context == null || context.isEmpty()) {
            return false;
        }
        
        // Check if already cleaned
        if (context.containsKey("_cleaned_at")) {
            return false;
        }
        
        boolean hasChanges = false;
        
        // Remove PII - personal contact information
        hasChanges |= context.remove("customer_email") != null;
        hasChanges |= context.remove("customer_phone") != null;
        hasChanges |= context.remove("customer_name") != null;
        hasChanges |= context.remove("customer_first_name") != null;
        hasChanges |= context.remove("customer_last_name") != null;
        
        // Remove address details
        hasChanges |= context.remove("address_full") != null;
        hasChanges |= context.remove("address_street") != null;
        hasChanges |= context.remove("address_district") != null;
        hasChanges |= context.remove("address_number") != null;
        hasChanges |= context.remove("address_postal_code") != null;
        hasChanges |= context.remove("address_reference") != null;
        
        // Remove highly sensitive portability data
        hasChanges |= context.remove("portability_nip") != null;
        hasChanges |= context.remove("portability_imei") != null;
        hasChanges |= context.remove("portability_phone") != null;
        
        // Remove payment URLs (contain sensitive tokens)
        hasChanges |= context.remove("checkout_session_url") != null;
        
        // Remove IMEI compatibility data
        hasChanges |= context.remove("imei_compatibility_message") != null;
        
        if (hasChanges) {
            // Mark as cleaned with timestamp
            context.put("_cleaned_at", LocalDateTime.now().toString());
            context.put("_retention_policy", RETENTION_DAYS + "_days");
            
            conversation.setContextData(context);
            conversationRepository.save(conversation);
            
            logger.info("Cleaned sensitive data from conversation: {}", conversation.getConversationId());
            return true;
        }
        
        return false;
    }
    
    /**
     * Manually trigger cleanup for a specific conversation
     * Useful for GDPR "right to be forgotten" requests
     */
    @Transactional
    public boolean cleanupConversation(String conversationId) {
        logger.info("Manual cleanup requested for conversation: {}", conversationId);
        
        return conversationRepository.findByConversationId(conversationId)
            .map(this::removeSensitiveFields)
            .orElse(false);
    }
    
    /**
     * Get statistics about conversations pending cleanup
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCleanupStats() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(RETENTION_DAYS);
        
        List<ChatConversation> oldConversations = conversationRepository
            .findByUpdatedAtBeforeAndIsActiveTrue(cutoffDate);
        
        long pendingCleanup = oldConversations.stream()
            .filter(conv -> conv.getContextData() != null && !conv.getContextData().containsKey("_cleaned_at"))
            .count();
        
        long alreadyCleaned = oldConversations.size() - pendingCleanup;
        
        return Map.of(
            "total_old_conversations", oldConversations.size(),
            "pending_cleanup", pendingCleanup,
            "already_cleaned", alreadyCleaned,
            "retention_days", RETENTION_DAYS,
            "cutoff_date", cutoffDate
        );
    }
}
