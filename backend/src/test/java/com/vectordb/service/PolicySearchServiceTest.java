package com.vectordb.service;

import com.vectordb.core.VectorMath;
import com.vectordb.model.DocItem;
import com.vectordb.model.VectorItem;
import com.vectordb.model.dto.response.PolicySearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Tests for policy-aware search functionality.
 * Verifies that retrieval returns policy metadata with similarity scores.
 */
class PolicySearchServiceTest {

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
    }

    /**
     * Test: Policy search returns results with policy metadata.
     */
    @Test
    void policySearchReturnsResultsWithPolicyMetadata() throws Exception {
        // Setup metadata store with policy documents
        Map<Integer, DocItem> metadataStore = getMetadataStore();

        metadataStore.put(1, DocItem.builder()
                .id(1)
                .documentId("doc-001")
                .chunkIndex(0)
                .chunkText("This is a policy chunk about access control")
                .policyName("Access Control Policy")
                .policyType("Access Control")
                .sectionNumber("1.0")
                .sectionTitle("Overview")
                .build());

        metadataStore.put(2, DocItem.builder()
                .id(2)
                .documentId("doc-001")
                .chunkIndex(1)
                .chunkText("Remote access procedures must follow protocol")
                .policyName("Access Control Policy")
                .policyType("Access Control")
                .sectionNumber("2.0")
                .sectionTitle("Remote Access")
                .build());

        // Setup mock to return vector results
        List<Double> queryEmbedding = createDummyEmbedding(768);
        List<VectorItem> vectorResults = List.of(
                VectorItem.builder().id(1).embedding(queryEmbedding).build(),
                VectorItem.builder().id(2).embedding(queryEmbedding).build()
        );
        
        when(docStore.search(any(List.class), anyInt(), anyString()))
                .thenReturn(vectorResults);

        // Search for policies
        List<PolicySearchResult> results = documentService.searchPolicies(
                queryEmbedding, "cosine", 10);

        // Verify results contain policy metadata
        assertFalse(results.isEmpty(), "Should return results");
        assertNotNull(results.get(0).getPolicyName(), "Should include policyName");
        assertNotNull(results.get(0).getPolicyType(), "Should include policyType");
        assertEquals("Access Control Policy", results.get(0).getPolicyName());
        assertEquals("Access Control", results.get(0).getPolicyType());
    }

    /**
     * Test: Similarity scores are preserved and valid (0-1).
     */
    @Test
    void similarityScoresArePreservedAndValid() throws Exception {
        Map<Integer, DocItem> metadataStore = getMetadataStore();

        metadataStore.put(1, DocItem.builder()
                .id(1)
                .documentId("doc-001")
                .chunkIndex(0)
                .chunkText("Access control policy details")
                .policyName("Access Policy")
                .policyType("Access Control")
                .build());

        List<Double> queryEmbedding = createDummyEmbedding(768);
        
        when(docStore.search(any(List.class), anyInt(), anyString()))
                .thenReturn(List.of(VectorItem.builder().id(1).embedding(queryEmbedding).build()));

        List<PolicySearchResult> results = documentService.searchPolicies(
                queryEmbedding, "cosine", 10);

        assertFalse(results.isEmpty());
        double similarity = results.get(0).getSimilarity();
        
        // Similarity should be between 0 and 1
        assertTrue(similarity >= 0.0 && similarity <= 1.0, 
                "Similarity should be in range [0, 1], got: " + similarity);
    }

    /**
     * Test: Top-K limit is respected.
     */
    @Test
    void topKLimitIsRespected() throws Exception {
        Map<Integer, DocItem> metadataStore = getMetadataStore();
        List<Double> queryEmbedding = createDummyEmbedding(768);

        List<VectorItem> vectorResults = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            metadataStore.put(i, DocItem.builder()
                    .id(i)
                    .documentId("doc-" + String.format("%03d", i))
                    .chunkIndex(0)
                    .chunkText("Policy chunk " + i)
                    .policyName("Policy " + i)
                    .policyType("Type A")
                    .build());
            vectorResults.add(VectorItem.builder().id(i).embedding(queryEmbedding).build());
        }

        when(docStore.search(any(List.class), anyInt(), anyString()))
                .thenReturn(vectorResults);

        List<PolicySearchResult> results = documentService.searchPolicies(
                queryEmbedding, "cosine", 3);

        assertEquals(3, results.size(), "Should return exactly 3 results");
    }

    /**
     * Test: Multiple policies can be retrieved.
     */
    @Test
    void multiplePoliciesCanBeRetrieved() throws Exception {
        Map<Integer, DocItem> metadataStore = getMetadataStore();
        List<Double> queryEmbedding = createDummyEmbedding(768);

        metadataStore.put(1, DocItem.builder()
                .id(1)
                .documentId("doc-001")
                .chunkIndex(0)
                .chunkText("Access control details")
                .policyName("Access Control Policy")
                .policyType("Access")
                .build());

        metadataStore.put(2, DocItem.builder()
                .id(2)
                .documentId("doc-002")
                .chunkIndex(0)
                .chunkText("Data protection guidelines")
                .policyName("Data Protection Policy")
                .policyType("Data Security")
                .build());

        metadataStore.put(3, DocItem.builder()
                .id(3)
                .documentId("doc-003")
                .chunkIndex(0)
                .chunkText("Incident response procedures")
                .policyName("Incident Response Policy")
                .policyType("Incident Management")
                .build());

        List<VectorItem> vectorResults = List.of(
                VectorItem.builder().id(1).embedding(queryEmbedding).build(),
                VectorItem.builder().id(2).embedding(queryEmbedding).build(),
                VectorItem.builder().id(3).embedding(queryEmbedding).build()
        );
        
        when(docStore.search(any(List.class), anyInt(), anyString()))
                .thenReturn(vectorResults);

        List<PolicySearchResult> results = documentService.searchPolicies(
                queryEmbedding, "cosine", 10);

        assertTrue(results.size() >= 3, "Should retrieve multiple policies");
    }

    /**
     * Test: Filtering by policyName works correctly.
     */
    @Test
    void filteringByPolicyNameWorksCorrectly() throws Exception {
        Map<Integer, DocItem> metadataStore = getMetadataStore();
        List<Double> queryEmbedding = createDummyEmbedding(768);

        metadataStore.put(1, DocItem.builder()
                .id(1)
                .documentId("doc-001")
                .chunkIndex(0)
                .chunkText("Access control")
                .policyName("Access Control Policy")
                .policyType("Access")
                .build());

        metadataStore.put(2, DocItem.builder()
                .id(2)
                .documentId("doc-002")
                .chunkIndex(0)
                .chunkText("Data protection")
                .policyName("Data Protection Policy")
                .policyType("Data")
                .build());

        List<VectorItem> vectorResults = List.of(
                VectorItem.builder().id(1).embedding(queryEmbedding).build(),
                VectorItem.builder().id(2).embedding(queryEmbedding).build()
        );
        
        when(docStore.search(any(List.class), anyInt(), anyString()))
                .thenReturn(vectorResults);

        List<PolicySearchResult> results = documentService.searchPolicies(
                queryEmbedding, "cosine", 10, "Access Control Policy", null);

        for (PolicySearchResult result : results) {
            assertEquals("Access Control Policy", result.getPolicyName());
        }
    }

    /**
     * Test: Filtering by policyType works correctly.
     */
    @Test
    void filteringByPolicyTypeWorksCorrectly() throws Exception {
        Map<Integer, DocItem> metadataStore = getMetadataStore();
        List<Double> queryEmbedding = createDummyEmbedding(768);

        metadataStore.put(1, DocItem.builder()
                .id(1)
                .documentId("doc-001")
                .chunkIndex(0)
                .chunkText("Access control")
                .policyName("Access Control Policy")
                .policyType("Access Control")
                .build());

        metadataStore.put(2, DocItem.builder()
                .id(2)
                .documentId("doc-002")
                .chunkIndex(0)
                .chunkText("Data protection")
                .policyName("Data Protection Policy")
                .policyType("Data Protection")
                .build());

        List<VectorItem> vectorResults = List.of(
                VectorItem.builder().id(1).embedding(queryEmbedding).build(),
                VectorItem.builder().id(2).embedding(queryEmbedding).build()
        );
        
        when(docStore.search(any(List.class), anyInt(), anyString()))
                .thenReturn(vectorResults);

        List<PolicySearchResult> results = documentService.searchPolicies(
                queryEmbedding, "cosine", 10, null, "Data Protection");

        for (PolicySearchResult result : results) {
            assertEquals("Data Protection", result.getPolicyType());
        }
    }

    /**
     * Test: Empty retrieval returns empty list (no matching documents).
     */
    @Test
    void emptyRetrievalReturnsEmptyList() throws Exception {
        List<Double> queryEmbedding = createDummyEmbedding(768);

        when(docStore.search(any(List.class), anyInt(), anyString()))
                .thenReturn(new ArrayList<>());

        List<PolicySearchResult> results = documentService.searchPolicies(
                queryEmbedding, "cosine", 10);

        assertTrue(results.isEmpty(), "Should return empty list when no policies exist");
    }

    /**
     * Test: Non-policy documents are excluded from search.
     */
    @Test
    void nonPolicyDocumentsAreExcludedFromSearch() throws Exception {
        Map<Integer, DocItem> metadataStore = getMetadataStore();
        List<Double> queryEmbedding = createDummyEmbedding(768);

        metadataStore.put(1, DocItem.builder()
                .id(1)
                .documentId("doc-001")
                .chunkIndex(0)
                .chunkText("Policy content")
                .policyName("Access Policy")
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

        List<VectorItem> vectorResults = List.of(
                VectorItem.builder().id(1).embedding(queryEmbedding).build(),
                VectorItem.builder().id(2).embedding(queryEmbedding).build()
        );
        
        when(docStore.search(any(List.class), anyInt(), anyString()))
                .thenReturn(vectorResults);

        List<PolicySearchResult> results = documentService.searchPolicies(
                queryEmbedding, "cosine", 10);

        assertEquals(1, results.size(), "Should exclude non-policy documents");
        assertEquals("Access Policy", results.get(0).getPolicyName());
    }

    /**
     * Test: Search preserves chunk information.
     */
    @Test
    void searchPreservesChunkInformation() throws Exception {
        Map<Integer, DocItem> metadataStore = getMetadataStore();
        List<Double> queryEmbedding = createDummyEmbedding(768);

        metadataStore.put(1, DocItem.builder()
                .id(1)
                .documentId("doc-001")
                .chunkIndex(5)
                .chunkText("This is chunk 5 of the policy")
                .policyName("Test Policy")
                .policyType("Test")
                .build());

        when(docStore.search(any(List.class), anyInt(), anyString()))
                .thenReturn(List.of(VectorItem.builder().id(1).embedding(queryEmbedding).build()));

        List<PolicySearchResult> results = documentService.searchPolicies(
                queryEmbedding, "cosine", 10);

        assertFalse(results.isEmpty());
        PolicySearchResult result = results.get(0);
        
        assertEquals(5, result.getChunkIndex());
        assertEquals("This is chunk 5 of the policy", result.getChunkText());
        assertEquals("doc-001", result.getDocumentId());
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
            embedding.add(0.1);  // Dummy normalized value
        }
        return embedding;
    }
}
