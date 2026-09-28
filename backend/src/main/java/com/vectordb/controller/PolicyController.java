// src/main/java/com/vectordb/controller/PolicyController.java
package com.vectordb.controller;

import com.vectordb.model.dto.request.InsertDocumentRequest;
import com.vectordb.model.dto.request.UploadPolicyRequest;
import com.vectordb.model.dto.response.PolicyResponse;
import com.vectordb.service.DocumentService;
import com.vectordb.service.PdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Policy-specific REST endpoints.
 * Handles secure policy document upload, retrieval, listing, and deletion.
 * Reuses the existing VectorDB ingestion pipeline.
 */
@Slf4j
@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PdfService pdfService;
    private final DocumentService documentService;

    /**
     * POST /api/policies/upload
     * 
     * Upload a security policy PDF with policy metadata.
     * The PDF is extracted, chunked, embedded, and stored with policy metadata.
     * 
     * Request: multipart/form-data
     *   - file: PDF file (required)
     *   - policyName: Policy name (required)
     *   - policyType: Policy type/category (required)
     *   - sectionNumber: Section ID (optional)
     *   - sectionTitle: Section title (optional)
     *
     * Response: { success, documentId, chunks, ids }
     * 
     * Errors:
     *   - 400: Missing required fields or invalid file
     *   - 503: Ollama unavailable
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadPolicy(
            @RequestParam("file") MultipartFile file,
            @RequestParam("policyName") String policyName,
            @RequestParam("policyType") String policyType,
            @RequestParam(value = "sectionNumber", required = false) String sectionNumber,
            @RequestParam(value = "sectionTitle", required = false) String sectionTitle) {

        // Validate file
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "file is required"));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "file must be a PDF"));
        }

        // Validate policy metadata
        if (policyName == null || policyName.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "policyName is required"));
        }

        if (policyType == null || policyType.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "policyType is required"));
        }

        try {
            // Extract text from PDF
            String text = pdfService.extractText(file);

            if (text == null || text.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No text extracted from PDF"));
            }

            // Create InsertDocumentRequest with policy metadata
            InsertDocumentRequest request = new InsertDocumentRequest();
            request.setTitle(policyName);  // Use policy name as document title
            request.setText(text);
            request.setPolicyName(policyName);
            request.setPolicyType(policyType);
            request.setSectionNumber(sectionNumber);
            request.setSectionTitle(sectionTitle);

            // Reuse existing ingestion pipeline
            List<Integer> ids = documentService.insertDocument(request);

            if (ids.isEmpty()) {
                return ResponseEntity.status(503)
                        .body(Map.of(
                                "error",
                                "Ollama unavailable. Install from https://ollama.com " +
                                        "then run: ollama pull nomic-embed-text"
                        ));
            }

            // Extract documentId from first chunk metadata
            String documentId = documentService.getDocItem(ids.get(0))
                    .map(item -> item.getDocumentId())
                    .orElse(null);

            log.info("Policy '{}' uploaded successfully with {} chunks", policyName, ids.size());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "documentId", documentId,
                    "chunks", ids.size(),
                    "ids", ids
            ));

        } catch (Exception e) {
            log.error("Policy upload failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process policy: " + e.getMessage()));
        }
    }

    /**
     * GET /api/policies
     * 
     * List all security policies.
     * Returns policy metadata without embeddings or detailed chunk content.
     * 
     * Response: { policies: [{ documentId, policyName, policyType, chunkCount }, ...] }
     */
    @GetMapping
    public ResponseEntity<?> listPolicies() {
        try {
            List<PolicyResponse> policies = documentService.listPolicies();
            return ResponseEntity.ok(Map.of("policies", policies, "count", policies.size()));
        } catch (Exception e) {
            log.error("Failed to list policies: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to list policies"));
        }
    }

    /**
     * GET /api/policies/{documentId}
     * 
     * Retrieve details of a specific policy by document ID.
     * Returns policy metadata without embeddings.
     * 
     * Response: { documentId, policyName, policyType, chunkCount }
     * 
     * Errors:
     *   - 404: Policy not found
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<?> getPolicyDetail(@PathVariable String documentId) {
        try {
            return documentService.getPolicyDetail(documentId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Failed to get policy detail for {}: {}", documentId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve policy"));
        }
    }

    /**
     * DELETE /api/policies/{documentId}
     * 
     * Delete a security policy and all its associated chunks.
     * 
     * Response: { success, chunksRemoved }
     * 
     * Errors:
     *   - 404: Policy not found
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<?> deletePolicy(@PathVariable String documentId) {
        try {
            int removed = documentService.deleteDocumentGroup(documentId);
            if (removed == 0) {
                return ResponseEntity.notFound().build();
            }

            log.info("Policy {} deleted successfully ({} chunks removed)", documentId, removed);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "chunksRemoved", removed
            ));
        } catch (Exception e) {
            log.error("Failed to delete policy {}: {}", documentId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to delete policy"));
        }
    }
}
