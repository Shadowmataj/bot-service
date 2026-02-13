package com.portability.bot_service.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Response for document operations")
public record DocumentResponse(
    @Schema(description = "ID of the document", example = "550e8400-e29b-41d4-a716-446655440000")
    String documentId,
    
    @Schema(description = "Content of the document", example = "Document content")
    String content,
    
    @Schema(description = "Metadata associated with the document")
    Map<String, Object> metadata,
    
    @Schema(description = "Message describing the result of the operation", example = "Document stored successfully")
    String message,
    
    @Schema(description = "Indicates if the operation was successful", example = "true")
    boolean success
) {}