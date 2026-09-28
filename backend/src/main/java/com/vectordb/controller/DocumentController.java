// src/main/java/com/vectordb/controller/DocumentController.java
package com.vectordb.controller;

import com.vectordb.model.dto.request.InsertDocumentRequest;
import com.vectordb.model.dto.response.DocumentListResponse;
import com.vectordb.model.dto.response.DocumentSummaryResponse;
import com.vectordb.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.vectordb.model.dto.TextSearchRequest;
import com.vectordb.service.OllamaService;
import com.vectordb.service.PdfService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final PdfService pdfService;
    private final DocumentService documentService;
    private final OllamaService ollamaService;
    /**
     * POST /api/documents
     * Body: { "title": "...", "text": "..." }
     *
     * 400 — title or text missing
     * 503 — Ollama unreachable
     * 200 — { chunks, ids, dims }
     */
    @PostMapping
    public ResponseEntity<?> insertDocument(@RequestBody InsertDocumentRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "title is required"));
        }
        if (request.getText() == null || request.getText().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "text is required"));
        }

        List<Integer> ids = documentService.insertDocument(request);

        if (ids.isEmpty()) {
            return ResponseEntity.status(503)
                    .body(Map.of(
                            "error",
                            "Ollama unavailable. Install from https://ollama.com " +
                                    "then run: ollama pull nomic-embed-text"
                    ));
        }

        return ResponseEntity.ok(Map.of(
                "chunks", ids.size(),
                "ids",    ids,
                "dims",   768
        ));
    }

    /**
     * GET /api/documents
     * Returns all stored chunks with preview and metadata.
     */
    @GetMapping
    public List<DocumentListResponse> listDocuments() {
        return documentService.listAll();
    }

    @PostMapping("/search")
    public ResponseEntity<?> searchDocuments(
            @RequestBody TextSearchRequest req) {

        if (req.getQuery() == null || req.getQuery().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "query must not be blank"));
        }

        double[] raw = ollamaService.embed(req.getQuery());

        if (raw.length == 0) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Ollama unavailable"));
        }

        List<Double> embedding = new ArrayList<>();

        for (double v : raw) {
            embedding.add(v);
        }

        return ResponseEntity.ok(
                documentService.search(
                        embedding,
                        req.getMetric(),
                        req.getK()
                )
        );
    }

    @PostMapping(
            value = "/upload",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<?> uploadPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "file is required"));
        }

        if (!file.getOriginalFilename()
                .toLowerCase()
                .endsWith(".pdf")) {

            return ResponseEntity.badRequest()
                    .body(Map.of("error", "must be a PDF"));
        }

        try {

            String text = pdfService.extractText(file);

            if (text.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "No text extracted from PDF"
                        ));
            }

            InsertDocumentRequest request =
                    new InsertDocumentRequest();

            request.setTitle(title);
            request.setText(text);

            List<Integer> ids =
                    documentService.insertDocument(request);

            return ResponseEntity.ok(Map.of(
                    "chunks", ids.size(),
                    "ids", ids
            ));

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error",
                            e.getMessage()
                    ));
        }
    }

    /**
     * DELETE /api/documents/{id}
     * Removes one chunk by vector ID.
     * 404 if ID does not exist in either store.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable int id) {
        boolean deleted = documentService.delete(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Step 13 — Document grouping & bulk delete (additive, Step 13 only)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * GET /api/documents/grouped
     * Returns chunks grouped by documentId for the Documents page card view.
     */
    @GetMapping("/grouped")
    public List<DocumentSummaryResponse> listGroupedDocuments() {
        return documentService.listGrouped();
    }

    /**
     * DELETE /api/documents/document/{documentId}
     * Removes every chunk belonging to a document (all vectors + metadata),
     * then persists once.
     * 404 if no chunks were found for that documentId.
     */
    @DeleteMapping("/document/{documentId}")
    public ResponseEntity<?> deleteDocumentGroup(@PathVariable String documentId) {
        int removed = documentService.deleteDocumentGroup(documentId);
        if (removed == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("ok", true, "chunksRemoved", removed));
    }
}