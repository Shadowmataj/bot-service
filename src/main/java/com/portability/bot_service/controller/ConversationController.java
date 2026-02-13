package com.portability.bot_service.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portability.bot_service.model.enm.ConversationState;
import com.portability.bot_service.service.ConversationStateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller for managing conversation states and context.
 * Provides endpoints for monitoring and controlling conversation flow.
 */
@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Conversation Management", description = "Endpoints for managing conversation states and context")
public class ConversationController {

    private final ConversationStateService stateService;

    public ConversationController(ConversationStateService stateService) {
        this.stateService = stateService;
    }

    @GetMapping("/{conversationId}/state")
    @Operation(summary = "Get current conversation state")
    public ResponseEntity<Map<String, Object>> getConversationState(
            @PathVariable String conversationId) {
        
        ConversationState state = stateService.getCurrentState(conversationId);
        return ResponseEntity.ok(Map.of(
                "conversationId", conversationId,
                "currentState", state
        ));
    }

    @PostMapping("/{conversationId}/state")
    @Operation(summary = "Update conversation state")
    public ResponseEntity<Map<String, Object>> updateConversationState(
            @PathVariable String conversationId,
            @RequestBody Map<String, String> request) {
        
        String newStateStr = request.get("state");
        ConversationState newState = ConversationState.valueOf(newStateStr);
        
        boolean success = stateService.transitionTo(conversationId, newState);
        
        return ResponseEntity.ok(Map.of(
                "success", success,
                "conversationId", conversationId,
                "newState", newState
        ));
    }

    @GetMapping("/{conversationId}/context")
    @Operation(summary = "Get all conversation context data")
    public ResponseEntity<Map<String, Object>> getConversationContext(
            @PathVariable String conversationId) {
        
        Map<String, Object> context = stateService.getAllContextData(conversationId);
        return ResponseEntity.ok(Map.of(
                "conversationId", conversationId,
                "context", context
        ));
    }

    @PostMapping("/{conversationId}/context")
    @Operation(summary = "Store conversation context data")
    public ResponseEntity<Map<String, String>> storeConversationContext(
            @PathVariable String conversationId,
            @RequestBody Map<String, Object> contextData) {
        
        stateService.storeContextData(conversationId, contextData);
        
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Context data stored successfully"
        ));
    }

    @GetMapping("/{conversationId}/stats")
    @Operation(summary = "Get conversation statistics")
    public ResponseEntity<Map<String, Object>> getConversationStats(
            @PathVariable String conversationId) {
        
        Map<String, Object> stats = stateService.getConversationStats(conversationId);
        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/{conversationId}")
    @Operation(summary = "Reset conversation to initial state")
    public ResponseEntity<Map<String, String>> resetConversation(
            @PathVariable String conversationId) {
        
        stateService.resetConversation(conversationId);
        
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Conversation reset successfully"
        ));
    }
}
