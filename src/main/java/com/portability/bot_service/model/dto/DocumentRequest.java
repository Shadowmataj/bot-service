package com.portability.bot_service.model.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to store a new document in the vector database")
public record DocumentRequest(
    @Schema(description = "Content of the document", example = "This is the document content", required = true)
    @NotBlank(message = "Document content is required")
    String content,
    
    @Schema(description = "Optional metadata for the document", example = "{\"category\": \"documentation\", \"author\": \"John Doe\"}")
    Map<String, Object> metadata
) {}
