package com.portability.bot_service.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.portability.bot_service.model.dto.DocumentRequest;
import com.portability.bot_service.model.dto.DocumentResponse;
import com.portability.bot_service.model.dto.DocumentUpdateRequest;

@Service
public class DocumentService {

    @Autowired
    private VectorStore vectorStore;

    /**
     * Stores a new document in the vector database
     * 
     * @param request Document request containing content and optional metadata
     * @return DocumentResponse with the created document ID and status
     */
    public DocumentResponse storeDocument(DocumentRequest request) {
        try {
            // Generate unique ID for the document
            String documentId = UUID.randomUUID().toString();

            // Prepare metadata
            Map<String, Object> metadata = new HashMap<>();
            if (request.metadata() != null) {
                metadata.putAll(request.metadata());
            }
            metadata.put("documentId", documentId);
            metadata.put("createdAt", System.currentTimeMillis());

            // Create the document
            Document document = new Document(documentId, request.content(), metadata);

            // Store in the vector store
            vectorStore.add(List.of(document));

            return new DocumentResponse(
                    documentId,
                    request.content(),
                    metadata,
                    "Document stored successfully",
                    true
            );

        } catch (Exception e) {
            return new DocumentResponse(
                    null,
                    null,
                    null,
                    "Error storing document: " + e.getMessage(),
                    false
            );
        }
    }

    /**
     * Updates an existing document in the vector database
     * 
     * @param request Document update request with document ID, new content and metadata
     * @return DocumentResponse with the update status
     */
    public DocumentResponse updateDocument(DocumentUpdateRequest request) {
        try {
            // First, delete the existing document
            vectorStore.delete(List.of(request.documentId()));

            // Prepare updated metadata
            Map<String, Object> metadata = new HashMap<>();
            if (request.metadata() != null) {
                metadata.putAll(request.metadata());
            }
            metadata.put("documentId", request.documentId());
            metadata.put("updatedAt", System.currentTimeMillis());

            // Create the updated document
            Document document = new Document(request.documentId(), request.content(), metadata);

            // Store the updated document
            vectorStore.add(List.of(document));

            return new DocumentResponse(
                    request.documentId(),
                    request.content(),
                    metadata,
                    "Document updated successfully",
                    true
            );

        } catch (Exception e) {
            return new DocumentResponse(
                    request.documentId(),
                    null,
                    null,
                    "Error updating document: " + e.getMessage(),
                    false
            );
        }
    }

    /**
     * Deletes a document from the vector database
     * 
     * @param documentId The ID of the document to delete
     * @return DocumentResponse with the deletion status
     */
    public DocumentResponse deleteDocument(String documentId) {
        try {
            vectorStore.delete(List.of(documentId));

            return new DocumentResponse(
                    documentId,
                    null,
                    null,
                    "Document deleted successfully",
                    true
            );
        } catch (Exception e) {
            return new DocumentResponse(
                    documentId,
                    null,
                    null,
                    "Error deleting document: " + e.getMessage(),
                    false
            );
        }
    }
}
