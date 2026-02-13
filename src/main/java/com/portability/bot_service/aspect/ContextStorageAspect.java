package com.portability.bot_service.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.portability.bot_service.service.ContextDataManager;
import com.portability.bot_service.service.ConversationContextHolder;

/**
 * Aspect that automatically captures tool responses and stores relevant data
 * in the conversation context. This enables the bot to maintain state and
 * reuse information across multiple interactions without asking the user again.
 * 
 * Order(2) ensures this runs after ToolExceptionHandlingAspect (Order 1)
 */
@Aspect
@Component
@Order(2)
public class ContextStorageAspect {

    private static final Logger logger = LoggerFactory.getLogger(ContextStorageAspect.class);

    private final ContextDataManager contextDataManager;

    public ContextStorageAspect(ContextDataManager contextDataManager) {
        this.contextDataManager = contextDataManager;
    }

    @Around("@annotation(tool)")
    public Object captureToolResponse(ProceedingJoinPoint joinPoint, Tool tool) throws Throwable {
        String toolName = tool.name();
        String conversationId = ConversationContextHolder.getConversationId();

        // Execute the tool
        Object result = joinPoint.proceed();

        // Only store context if we have a conversation ID and a successful result
        if (conversationId != null && result != null) {
            try {
                logger.debug("Capturing tool response for tool: {} in conversation: {}", toolName, conversationId);
                contextDataManager.processToolResponse(conversationId, toolName, result);
            } catch (Exception e) {
                // Log error but don't fail the tool execution
                logger.error("Failed to store context data for tool: {}", toolName, e);
            }
        } else {
            if (conversationId == null) {
                logger.warn("No conversation ID set for tool: {}. Context data will not be stored.", toolName);
            }
        }

        return result;
    }
}
