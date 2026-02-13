package com.portability.bot_service.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

@Schema(description = "Request to update an existing document in the vector database")
public record DocumentUpdateRequest(
    @Schema(description = "ID of the document to update", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
    @NotBlank(message = "Document ID is required")
    String documentId,
    
    @Schema(description = "New content for the document", example = "Updated document content", required = true)
    @NotBlank(message = "Document content is required")
    String content,
    
    @Schema(description = "Optional updated metadata for the document", example = "{\"category\": \"updated\", \"version\": 2}")
    Map<String, Object> metadata
) {}
