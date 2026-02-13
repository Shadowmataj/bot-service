package com.portability.bot_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portability.bot_service.model.dto.DocumentRequest;
import com.portability.bot_service.model.dto.DocumentResponse;
import com.portability.bot_service.model.dto.DocumentUpdateRequest;
import com.portability.bot_service.service.DocumentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Document Management", description = "Endpoints for managing documents in the vector database")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Operation(
        summary = "Store a new document",
        description = "Stores a new document in the vector database with optional metadata. Returns the generated document ID."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Document created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = DocumentResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request body",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = DocumentResponse.class))
        )
    })
    @PostMapping
    public ResponseEntity<DocumentResponse> storeDocument(
            @Parameter(description = "Document content and optional metadata", required = true)
            @Valid @RequestBody DocumentRequest request) {
        DocumentResponse response = documentService.storeDocument(request);
        
        if (response.success()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Operation(
        summary = "Update an existing document",
        description = "Updates an existing document in the vector database by its ID. Replaces the content and metadata."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Document updated successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = DocumentResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request body",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Document not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = DocumentResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = DocumentResponse.class))
        )
    })
    @PutMapping
    public ResponseEntity<DocumentResponse> updateDocument(
            @Parameter(description = "Document ID, new content and optional metadata", required = true)
            @Valid @RequestBody DocumentUpdateRequest request) {
        DocumentResponse response = documentService.updateDocument(request);
        
        if (response.success()) {
            return ResponseEntity.ok(response);
        } else if (response.message().contains("not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Operation(
        summary = "Delete a document",
        description = "Deletes a document from the vector database by its ID."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Document deleted successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = DocumentResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Document not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = DocumentResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = DocumentResponse.class))
        )
    })
    @DeleteMapping("/{documentId}")
    public ResponseEntity<DocumentResponse> deleteDocument(
            @Parameter(description = "ID of the document to delete", required = true)
            @PathVariable String documentId) {
        DocumentResponse response = documentService.deleteDocument(documentId);
        
        if (response.success()) {
            return ResponseEntity.ok(response);
        } else if (response.message().contains("not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}