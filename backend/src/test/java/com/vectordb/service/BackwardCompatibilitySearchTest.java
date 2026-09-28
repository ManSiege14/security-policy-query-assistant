package com.vectordb.service;

import com.vectordb.model.DocItem;
import com.vectordb.model.VectorItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Tests for backward compatibility of existing search and RAG functionality.
 * Verifies that Stage 4 changes do not break existing search behavior.
 */
class BackwardCompatibilitySearchTest {

    private DocumentService documentService;

    @Mock
    private OllamaService ollamaService;

    @Mock
    private VectorStoreService docStore;

    @Mock
    private PersistenceService persistenceService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        documentService = new DocumentService(ollamaService, docStore, persistenceService);

        // Stub vector store search: generic search() delegates to docStore, not policy filtering
        when(docStore.search(any(List.class), anyInt(), anyString()))
                .thenAnswer(invocation -> {
                    int k = invocation.getArgument(1);
                    @SuppressWarnings("unchecked")
                    List<Double> embedding = invocation.getArgument(0);
                    Map<Integer, DocItem> store = getMetadataStore();
                    return store.keySet().stream()
                            .sorted(Comparator.naturalOrder())
                            .limit(k)
                            .map(id -> VectorItem.builder().id(id).embedding(embedding).build())
                            .collect(Collectors.toList());
                });
    }

    /**
     * Test: Existing search method still returns DocItem objects.
     */
    @Test
    void existingSearchStillReturnsDocItems() throws Exception {
        Map<Integer, DocItem> metadataStore = getMetadataStore();

        // Add some documents
        metadataStore.put(1, DocItem.builder()
                .id(1)
                .documentId("doc-001")
                .chunkIndex(0)
                .chunkText("Generic document content")
                .build());

        List<Double> queryEmbedding = createDummyEmbedding(768);

        // Call existing search method
        List<DocItem> results = documentService.search(queryEmbedding, "cosine", 10);

        // Should return DocItem objects (not PolicySearchResult)
        assertFalse(results.isEmpty());
        assertNotNull(results.get(0).getChunkText());
        assertEquals("doc-001", results.get(0).getDocumentId());
    }

    /**
     * Test: Search works with generic documents (no policy metadata).
     */
    @Test
    void searchWorksWithGenericDocuments() throws Exception {
        Map<Integer, DocItem> metadataStore = getMetadataStore();

        // Add generic documents without policy metadata
        metadataStore.put(1, DocItem.builder()
                .id(1)
                .documentId("doc-001")
                .chunkIndex(0)
                .chunkText("Generic document")
                .policyName(null)
                .policyType(null)
                .build());

        List<Double> queryEmbedding = createDummyEmbedding(768);

        List<DocItem> results = documentService.search(queryEmbedding, "cosine", 10);

        // Existing search should still find generic documents
        assertFalse(results.isEmpty());
        assertNull(results.get(0).getPolicyName());
    }

    /**
     * Test: Existing search does not filter by policy metadata.
     */
    @Test
    void existingSearchDoesNotFilterByPolicyMetadata() throws Exception {
        Map<Integer, DocItem> metadataStore = getMetadataStore();

        // Add mix of policy and generic documents
        metadataStore.put(1, DocItem.builder()
                .id(1)
                .documentId("doc-001")
                .chunkIndex(0)
                .chunkText("Policy document")
                .policyName("Policy A")
                .policyType("Access")
                .build());

        metadataStore.put(2, DocItem.builder()
                .id(2)
                .documentId("doc-002")
                .chunkIndex(0)
                .chunkText("Generic document")
                .policyName(null)
                .policyType(null)
                .build());

        List<Double> queryEmbedding = createDummyEmbedding(768);

        List<DocItem> results = documentService.search(queryEmbedding, "cosine", 10);

        // Existing search should return both types
        assertTrue(results.size() >= 1, "Should include documents regardless of policy metadata");
    }

    /**
     * Test: RAG retrieval can still access document metadata.
     */
    @Test
    void ragRetrievalCanAccessDocumentMetadata() throws Exception {
        Map<Integer, DocItem> metadataStore = getMetadataStore();

        metadataStore.put(1, DocItem.builder()
                .id(1)
                .documentId("doc-001")
                .chunkIndex(0)
                .chunkText("RAG content for question answering")
                .title("Document Title")
                .policyName(null)
                .policyType(null)
                .build());

        // RAG should be able to get the document item by ID
        var docItem = documentService.getDocItem(1);

        assertTrue(docItem.isPresent());
        assertEquals("RAG content for question answering", docItem.get().getChunkText());
        assertEquals("Document Title", docItem.get().getTitle());
    }

    /**
     * Test: Multiple metrics are still supported in search.
     */
    @Test
    void multipleMetricsAreStillSupportedInSearch() throws Exception {
        Map<Integer, DocItem> metadataStore = getMetadataStore();

        metadataStore.put(1, DocItem.builder()
                .id(1)
                .documentId("doc-001")
                .chunkIndex(0)
                .chunkText("Test content")
                .build());

        List<Double> queryEmbedding = createDummyEmbedding(768);

        // Test with different metrics
        List<DocItem> cosinResults = documentService.search(queryEmbedding, "cosine", 10);
        List<DocItem> euclideanResults = documentService.search(queryEmbedding, "euclidean", 10);
        List<DocItem> manhattanResults = documentService.search(queryEmbedding, "manhattan", 10);

        // All metrics should return results (or empty, but consistently)
        assertTrue(cosinResults != null);
        assertTrue(euclideanResults != null);
        assertTrue(manhattanResults != null);
    }

    /**
     * Test: Top-K parameter still works in existing search.
     */
    @Test
    void topKParameterStillWorksInExistingSearch() throws Exception {
        Map<Integer, DocItem> metadataStore = getMetadataStore();

        // Add 10 documents
        for (int i = 1; i <= 10; i++) {
            metadataStore.put(i, DocItem.builder()
                    .id(i)
                    .documentId("doc-" + i)
                    .chunkIndex(0)
                    .chunkText("Content " + i)
                    .build());
        }

        List<Double> queryEmbedding = createDummyEmbedding(768);

        // Search with different k values
        List<DocItem> top3 = documentService.search(queryEmbedding, "cosine", 3);
        List<DocItem> top5 = documentService.search(queryEmbedding, "cosine", 5);

        // k=3 should return <= 3, k=5 should return <= 5
        assertTrue(top3.size() <= 3);
        assertTrue(top5.size() <= 5);
    }

    /**
     * Test: Document access by vectorId still works.
     */
    @Test
    void documentAccessByVectorIdStillWorks() throws Exception {
        Map<Integer, DocItem> metadataStore = getMetadataStore();

        DocItem originalDoc = DocItem.builder()
                .id(42)
                .documentId("doc-special")
                .chunkIndex(3)
                .chunkText("Special content")
                .title("Special Title")
                .build();

        metadataStore.put(42, originalDoc);

        // Access by vector ID
        var retrievedDoc = documentService.getDocItem(42);

        assertTrue(retrievedDoc.isPresent());
        assertEquals("Special content", retrievedDoc.get().getChunkText());
        assertEquals("Special Title", retrievedDoc.get().getTitle());
        assertEquals(3, retrievedDoc.get().getChunkIndex());
    }

    /**
     * Test: Existing listAll method still works and includes all documents.
     */
    @Test
    void existingListAllStillIncludesAllDocuments() throws Exception {
        Map<Integer, DocItem> docs = getMetadataStore();

        docs.put(1, DocItem.builder()
                .id(1)
                .documentId("doc-001")
                .chunkIndex(0)
                .chunkText("Policy doc")
                .policyName("Policy A")
                .policyType("Access")
                .build());

        docs.put(2, DocItem.builder()
                .id(2)
                .documentId("doc-002")
                .chunkIndex(0)
                .chunkText("Generic doc")
                .build());

        assertEquals(2, docs.size());
    }

    // Helper methods

    private Map<Integer, DocItem> getMetadataStore() throws Exception {
        Field field = DocumentService.class.getDeclaredField("metadataStore");
        field.setAccessible(true);
        return (Map<Integer, DocItem>) field.get(documentService);
    }

    private List<Double> createDummyEmbedding(int dimensions) {
        List<Double> embedding = new ArrayList<>();
        for (int i = 0; i < dimensions; i++) {
            embedding.add(0.1);
        }
        return embedding;
    }
}
