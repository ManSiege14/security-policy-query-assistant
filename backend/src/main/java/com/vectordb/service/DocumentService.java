// src/main/java/com/vectordb/service/DocumentService.java
package com.vectordb.service;

import com.vectordb.core.TextChunker;
import com.vectordb.model.DocItem;
import com.vectordb.model.VectorItem;
import com.vectordb.model.dto.request.InsertDocumentRequest;
import com.vectordb.model.dto.response.DocumentListResponse;
import com.vectordb.model.dto.response.DocumentSummaryResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DocumentService {

    private final OllamaService ollamaService;
    private final VectorStoreService docStore;
    private final PersistenceService persistenceService;

    // Parallel metadata store: vectorId -> DocItem
    private final Map<Integer, DocItem> metadataStore = new ConcurrentHashMap<>();

    public DocumentService(
            OllamaService ollamaService,
            @Qualifier("docVectorStore") VectorStoreService docStore,
            PersistenceService persistenceService) {

        this.ollamaService = ollamaService;
        this.docStore = docStore;
        this.persistenceService = persistenceService;
    }

    /**
     * PostConstruct initialization hook to sync in-memory stores with local disk snapshots
     * right when the bean is spun up by Spring's ApplicationContext.
     */
    @PostConstruct
    public void loadPersistedData() {
        try {
            Map<Integer, VectorItem> vectors = persistenceService.loadVectors();
            Map<Integer, DocItem> documents = persistenceService.loadDocuments();

            docStore.loadStore(vectors);

            metadataStore.clear();
            metadataStore.putAll(documents);

            log.info("Loaded {} vectors and {} documents from disk",
                    vectors.size(),
                    documents.size());
        } catch (Exception e) {
            log.error("Failed to load persisted data", e);
        }
    }

    /**
     * Full pipeline: chunk -> embed -> store -> single-pass write to disk.
     */
    public List<Integer> insertDocument(InsertDocumentRequest request) {
        String title = request.getTitle();
        String text  = request.getText();

        List<String> chunks = TextChunker.chunk(text);
        log.info("'{}' split into {} chunk(s)", title, chunks.size());

        String documentId = UUID.randomUUID().toString();
        List<Integer> insertedIds = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);

            double[] raw = ollamaService.embed(chunk);
            if (raw.length == 0) {
                log.warn("Embedding failed at chunk {}/{} for '{}' — aborting batch insertion",
                        i + 1, chunks.size(), title);

                // Save whatever successfully parsed up to this point
                triggerAutoSave();
                return insertedIds;
            }

            List<Double> embedding = new ArrayList<>(raw.length);
            for (double v : raw) {
                embedding.add(v);
            }

            String chunkTitle = chunks.size() > 1
                    ? title + " [" + (i + 1) + "/" + chunks.size() + "]"
                    : title;

            int vectorId = docStore.insert(chunkTitle, "doc", embedding);

            DocItem docItem = DocItem.builder()
                    .id(vectorId)
                    .documentId(documentId)
                    .chunkIndex(i)
                    .title(chunkTitle)
                    .chunkText(chunk)
                    .policyName(request.getPolicyName())
                    .policyType(request.getPolicyType())
                    .sectionNumber(request.getSectionNumber())
                    .sectionTitle(request.getSectionTitle())
                    .build();

            metadataStore.put(vectorId, docItem);
            insertedIds.add(vectorId);

            log.debug("Chunk {}/{} staged in-memory as vectorId={}", i + 1, chunks.size(), vectorId);
        }

        // Save exactly ONCE after the entire file array processes cleanly
        triggerAutoSave();
        return insertedIds;
    }

    /**
     * Deletes a chunk by vector ID from both stores and triggers a disk rewrite.
     */
    public boolean delete(int id) {
        boolean vectorRemoved = docStore.delete(id);
        boolean metaRemoved   = metadataStore.remove(id) != null;

        triggerAutoSave();

        return vectorRemoved && metaRemoved;
    }

    public List<DocItem> search(List<Double> embedding, String metric, int k) {
        List<VectorItem> matches = docStore.search(embedding, k, metric);
        List<DocItem> results = new ArrayList<>();

        for (VectorItem item : matches) {
            DocItem doc = metadataStore.get(item.getId());
            if (doc != null) {
                results.add(doc);
            }
        }
        return results;
    }

    public Optional<DocItem> getDocItem(int vectorId) {
        return Optional.ofNullable(metadataStore.get(vectorId));
    }

    public VectorStoreService getDocStore() {
        return docStore;
    }

    public int size() {
        return metadataStore.size();
    }

    // Helper method to consolidate safe disk syncing
    private void triggerAutoSave() {
        try {
            persistenceService.saveAll(docStore.getStore(), metadataStore);
        } catch (Exception e) {
            log.error("Critical: Failed to sync database updates to disk persistence layers!", e);
        }
    }

    private DocumentListResponse toListResponse(DocItem item) {
        String text    = item.getChunkText();
        String preview = text.length() > 120 ? text.substring(0, 120) + "…" : text;
        int wordCount  = text.isBlank() ? 0 : text.trim().split("\\s+").length;

        return DocumentListResponse.builder()
                .id(item.getId())
                .documentId(item.getDocumentId())
                .chunkIndex(item.getChunkIndex())
                .title(item.getTitle())
                .preview(preview)
                .wordCount(wordCount)
                .policyName(item.getPolicyName())
                .policyType(item.getPolicyType())
                .sectionNumber(item.getSectionNumber())
                .sectionTitle(item.getSectionTitle())
                .build();
    }

    public List<DocumentListResponse> listAll() {
        return metadataStore.values().stream()
                .sorted(Comparator.comparingInt(DocItem::getId))
                .map(this::toListResponse)
                .collect(Collectors.toList());
    }
// ─────────────────────────────────────────────────────────────────────
    // Step 13 — Document grouping & bulk delete (additive, Step 13 only)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Groups stored chunks by documentId for the Documents page card view.
     * Reuses metadataStore — no new storage, no duplication.
     * Order: documents appear in the order their first chunk was inserted
     * (vector IDs increase monotonically on insert, so sorting by id
     * before grouping preserves insertion order).
     */
    public List<DocumentSummaryResponse> listGrouped() {
        List<DocItem> orderedItems = metadataStore.values().stream()
                .filter(item -> item.getDocumentId() != null)
                .sorted(Comparator.comparingInt(DocItem::getId))
                .collect(Collectors.toList());

        Map<String, List<DocItem>> byDocId = new LinkedHashMap<>();
        for (DocItem item : orderedItems) {
            byDocId.computeIfAbsent(item.getDocumentId(), k -> new ArrayList<>())
                    .add(item);
        }

        List<DocumentSummaryResponse> summaries = new ArrayList<>();

        for (Map.Entry<String, List<DocItem>> entry : byDocId.entrySet()) {
            String documentId = entry.getKey();
            List<DocItem> chunks = entry.getValue();

            chunks.sort(Comparator.comparingInt(DocItem::getChunkIndex));

            String baseTitle = stripChunkSuffix(chunks.get(0).getTitle());

            List<DocumentListResponse> chunkResponses = chunks.stream()
                    .map(this::toListResponse)
                    .collect(Collectors.toList());

            summaries.add(DocumentSummaryResponse.builder()
                    .documentId(documentId)
                    .title(baseTitle)
                    .totalChunks(chunks.size())
                    .chunks(chunkResponses)
                    .build());
        }

        return summaries;
    }

    /**
     * Removes the " [i/n]" chunk-index suffix appended in insertDocument(),
     * using plain string operations (no regex).
     * Suffix format is always: " [" + number + "/" + number + "]"
     */
    private String stripChunkSuffix(String title) {
        int bracketStart = title.lastIndexOf(" [");
        if (bracketStart == -1 || !title.endsWith("]")) {
            return title;
        }
        return title.substring(0, bracketStart);
    }

    /**
     * Deletes every chunk belonging to a documentId from docStore and
     * metadataStore, then persists once via the existing triggerAutoSave()
     * helper — no changes to persistence logic itself.
     * Returns the number of chunks removed (0 means the documentId did not exist).
     */
    public int deleteDocumentGroup(String documentId) {
        List<Integer> idsToRemove = metadataStore.values().stream()
                .filter(item -> item.getDocumentId().equals(documentId))
                .map(DocItem::getId)
                .collect(Collectors.toList());

        for (int vectorId : idsToRemove) {
            docStore.delete(vectorId);
            metadataStore.remove(vectorId);
        }

        if (!idsToRemove.isEmpty()) {
            triggerAutoSave();
        }

        return idsToRemove.size();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Policy-specific operations (Stage 3)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Returns information about a specific policy document by documentId.
     * Includes policy metadata and chunk count, but not embeddings or full chunk text.
     */
    public Optional<com.vectordb.model.dto.response.PolicyResponse> getPolicyDetail(String documentId) {
        List<DocItem> chunks = metadataStore.values().stream()
                .filter(item -> item.getDocumentId().equals(documentId))
                .sorted(Comparator.comparingInt(DocItem::getId))
                .collect(Collectors.toList());

        if (chunks.isEmpty()) {
            return Optional.empty();
        }

        // Use first chunk's policy metadata (all chunks from same document should have same policy name/type)
        DocItem firstChunk = chunks.get(0);

        return Optional.of(com.vectordb.model.dto.response.PolicyResponse.builder()
                .documentId(documentId)
                .policyName(firstChunk.getPolicyName())
                .policyType(firstChunk.getPolicyType())
                .chunkCount(chunks.size())
                .build());
    }

    /**
     * Lists all unique policies (grouped by documentId and policy metadata).
     * Returns only one entry per policy document, regardless of chunk count.
     * Does not include embeddings or detailed chunk content.
     */
    public List<com.vectordb.model.dto.response.PolicyResponse> listPolicies() {
        List<DocItem> orderedItems = metadataStore.values().stream()
                .sorted(Comparator.comparingInt(DocItem::getId))
                .collect(Collectors.toList());

        Map<String, com.vectordb.model.dto.response.PolicyResponse> policies = new LinkedHashMap<>();

        for (DocItem item : orderedItems) {
            String docId = item.getDocumentId();
            
            // Only add if policy metadata is present (not a generic document)
            if (docId != null && item.getPolicyName() != null && item.getPolicyType() != null) {
                if (!policies.containsKey(docId)) {
                    policies.put(docId, com.vectordb.model.dto.response.PolicyResponse.builder()
                            .documentId(docId)
                            .policyName(item.getPolicyName())
                            .policyType(item.getPolicyType())
                            .chunkCount(0)
                            .build());
                }
                
                // Increment chunk count for this policy
                com.vectordb.model.dto.response.PolicyResponse policy = policies.get(docId);
                policy.setChunkCount(policy.getChunkCount() + 1);
            }
        }

        return new ArrayList<>(policies.values());
    }

    /**
     * Searches for policy documents with similarity scores.
     * Results are sorted by similarity (highest first).
     * Optionally filters by policyName and/or policyType.
     *
     * @param embedding Query embedding vector
     * @param metric    Distance metric ("cosine", "euclidean", "manhattan")
     * @param k         Maximum number of results to return
     * @param policyName Optional filter (null = no filter)
     * @param policyType Optional filter (null = no filter)
     * @return List of policy search results sorted by similarity (descending)
     */
    public List<com.vectordb.model.dto.response.PolicySearchResult> searchPolicies(
            List<Double> embedding,
            String metric,
            int k,
            String policyName,
            String policyType) {

        // Step 1: Get all vector store results sorted by distance
        List<com.vectordb.model.VectorItem> vectorResults = docStore.search(embedding, Math.max(k, 100), metric);

        // Step 2: Convert to array for distance calculations
        double[] queryArr = com.vectordb.core.VectorMath.toArray(embedding);

        // Step 3: Build PolicySearchResult objects with similarity scores and metadata
        List<com.vectordb.model.dto.response.PolicySearchResult> results = new ArrayList<>();

        for (com.vectordb.model.VectorItem vectorItem : vectorResults) {
            // Get the DocItem metadata for this vector
            DocItem docItem = metadataStore.get(vectorItem.getId());
            if (docItem == null) {
                continue;
            }

            // Only include results with policy metadata
            if (docItem.getPolicyName() == null || docItem.getPolicyType() == null) {
                continue;
            }

            // Apply optional policyName filter
            if (policyName != null && !policyName.isBlank() 
                    && !docItem.getPolicyName().equalsIgnoreCase(policyName)) {
                continue;
            }

            // Apply optional policyType filter
            if (policyType != null && !policyType.isBlank() 
                    && !docItem.getPolicyType().equalsIgnoreCase(policyType)) {
                continue;
            }

            // Calculate similarity: convert distance to similarity (0-1, higher = more relevant)
            double distance = com.vectordb.core.VectorMath.distance(
                    queryArr,
                    com.vectordb.core.VectorMath.toArray(vectorItem.getEmbedding()),
                    metric
            );
            // For cosine distance, similarity = 1 - distance; clamp for user-facing [0, 1] (float noise)
            double similarity = Math.max(0.0, Math.min(1.0, 1.0 - distance));

            // Build result with policy metadata and similarity
            com.vectordb.model.dto.response.PolicySearchResult result =
                    com.vectordb.model.dto.response.PolicySearchResult.builder()
                            .documentId(docItem.getDocumentId())
                            .policyName(docItem.getPolicyName())
                            .policyType(docItem.getPolicyType())
                            .sectionNumber(docItem.getSectionNumber())
                            .sectionTitle(docItem.getSectionTitle())
                            .chunkIndex(docItem.getChunkIndex())
                            .chunkText(docItem.getChunkText())
                            .similarity(similarity)
                            .build();

            results.add(result);

            // Stop after k results
            if (results.size() >= k) {
                break;
            }
        }

        // Results are already sorted by similarity because docStore.search() sorts by distance
        // (and we break after k results, preserving the order)
        return results;
    }

    /**
     * Searches for policy documents without filters.
     * Convenience method that calls searchPolicies with null filters.
     */
    public List<com.vectordb.model.dto.response.PolicySearchResult> searchPolicies(
            List<Double> embedding,
            String metric,
            int k) {
        return searchPolicies(embedding, metric, k, null, null);
    }
}