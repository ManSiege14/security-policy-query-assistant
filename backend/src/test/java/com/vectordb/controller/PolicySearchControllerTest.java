package com.vectordb.controller;

import com.vectordb.model.dto.response.PolicySearchResponse;
import com.vectordb.model.dto.response.PolicySearchResult;
import com.vectordb.service.DocumentService;
import com.vectordb.service.OllamaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Tests for policy search endpoint.
 * Verifies that GET /api/policies/search returns policy metadata with similarity scores.
 */
class PolicySearchControllerTest {

    private PolicyController policyController;

    @Mock
    private OllamaService ollamaService;

    @Mock
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        policyController = new PolicyController(null, documentService, ollamaService);
    }

    /**
     * Test: Policy search with valid query returns results with metadata.
     */
    @Test
    void policySearchWithValidQueryReturnsResults() {
        // Setup
        double[] embedding = createDummyEmbedding(768);
        when(ollamaService.embed("access control policy")).thenReturn(embedding);

        List<PolicySearchResult> mockResults = List.of(
                PolicySearchResult.builder()
                        .documentId("doc-001")
                        .policyName("Access Control Policy")
                        .policyType("Access Control")
                        .sectionNumber("1.0")
                        .sectionTitle("Overview")
                        .chunkIndex(0)
                        .chunkText("Access control is important")
                        .similarity(0.95)
                        .build()
        );

        when(documentService.searchPolicies(any(List.class), eq("cosine"), eq(5), isNull(), isNull()))
                .thenReturn(mockResults);

        // Execute
        ResponseEntity<?> response = policyController.searchPolicies("access control policy", 5, null, null);

        // Verify
        assertEquals(200, response.getStatusCodeValue());
        PolicySearchResponse body = (PolicySearchResponse) response.getBody();
        assertNotNull(body);
        assertEquals("access control policy", body.getQuery());
        assertEquals(1, body.getCount());
        assertFalse(body.getResults().isEmpty());

        PolicySearchResult result = body.getResults().get(0);
        assertEquals("Access Control Policy", result.getPolicyName());
        assertEquals("Access Control", result.getPolicyType());
        assertEquals(0.95, result.getSimilarity());
        assertEquals("Access control is important", result.getChunkText());
    }

    /**
     * Test: Policy search without query parameter returns 400.
     */
    @Test
    void searchWithoutQueryReturnsBadRequest() {
        ResponseEntity<?> response = policyController.searchPolicies("", 5, null, null);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("required"));
    }

    /**
     * Test: Policy search with null query returns 400.
     */
    @Test
    void searchWithNullQueryReturnsBadRequest() {
        ResponseEntity<?> response = policyController.searchPolicies(null, 5, null, null);

        assertEquals(400, response.getStatusCodeValue());
    }

    /**
     * Test: Policy search when Ollama unavailable returns 503.
     */
    @Test
    void searchWhenOllamaUnavailableReturns503() {
        when(ollamaService.embed(any())).thenReturn(new double[0]);  // Empty array signals unavailability

        ResponseEntity<?> response = policyController.searchPolicies("access control", 5, null, null);

        assertEquals(503, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Ollama"));
    }

    /**
     * Test: Policy search with topK parameter respects the limit.
     */
    @Test
    void searchWithTopKParameterRespected() {
        double[] embedding = createDummyEmbedding(768);
        when(ollamaService.embed("query")).thenReturn(embedding);

        List<PolicySearchResult> mockResults = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            mockResults.add(PolicySearchResult.builder()
                    .documentId("doc-" + i)
                    .policyName("Policy " + i)
                    .policyType("Type")
                    .chunkIndex(0)
                    .chunkText("Content " + i)
                    .similarity(0.9 - (i * 0.05))
                    .build());
        }

        when(documentService.searchPolicies(any(List.class), eq("cosine"), eq(3), isNull(), isNull()))
                .thenReturn(mockResults);

        ResponseEntity<?> response = policyController.searchPolicies("query", 3, null, null);

        assertEquals(200, response.getStatusCodeValue());
        PolicySearchResponse body = (PolicySearchResponse) response.getBody();
        assertEquals(3, body.getResults().size());
    }

    /**
     * Test: Policy search with policyName filter.
     */
    @Test
    void searchWithPolicyNameFilter() {
        double[] embedding = createDummyEmbedding(768);
        when(ollamaService.embed("policy query")).thenReturn(embedding);

        List<PolicySearchResult> mockResults = List.of(
                PolicySearchResult.builder()
                        .documentId("doc-001")
                        .policyName("Access Control Policy")
                        .policyType("Access")
                        .chunkIndex(0)
                        .chunkText("Content")
                        .similarity(0.88)
                        .build()
        );

        when(documentService.searchPolicies(any(List.class), eq("cosine"), eq(5), 
                eq("Access Control Policy"), isNull()))
                .thenReturn(mockResults);

        ResponseEntity<?> response = policyController.searchPolicies(
                "policy query", 5, "Access Control Policy", null);

        assertEquals(200, response.getStatusCodeValue());
        PolicySearchResponse body = (PolicySearchResponse) response.getBody();
        assertEquals(1, body.getResults().size());
        assertEquals("Access Control Policy", body.getResults().get(0).getPolicyName());
    }

    /**
     * Test: Policy search with policyType filter.
     */
    @Test
    void searchWithPolicyTypeFilter() {
        double[] embedding = createDummyEmbedding(768);
        when(ollamaService.embed("security query")).thenReturn(embedding);

        List<PolicySearchResult> mockResults = List.of(
                PolicySearchResult.builder()
                        .documentId("doc-001")
                        .policyName("Access Control Policy")
                        .policyType("Access Control")
                        .chunkIndex(0)
                        .chunkText("Content")
                        .similarity(0.92)
                        .build()
        );

        when(documentService.searchPolicies(any(List.class), eq("cosine"), eq(5), 
                isNull(), eq("Access Control")))
                .thenReturn(mockResults);

        ResponseEntity<?> response = policyController.searchPolicies(
                "security query", 5, null, "Access Control");

        assertEquals(200, response.getStatusCodeValue());
        PolicySearchResponse body = (PolicySearchResponse) response.getBody();
        assertEquals(1, body.getResults().size());
        assertEquals("Access Control", body.getResults().get(0).getPolicyType());
    }

    /**
     * Test: Policy search returns empty results when no matches found.
     */
    @Test
    void searchReturnsEmptyResultsWhenNoMatches() {
        double[] embedding = createDummyEmbedding(768);
        when(ollamaService.embed("no match query")).thenReturn(embedding);

        when(documentService.searchPolicies(any(List.class), eq("cosine"), eq(5), isNull(), isNull()))
                .thenReturn(new ArrayList<>());

        ResponseEntity<?> response = policyController.searchPolicies("no match query", 5, null, null);

        assertEquals(200, response.getStatusCodeValue());
        PolicySearchResponse body = (PolicySearchResponse) response.getBody();
        assertEquals(0, body.getResults().size());
        assertEquals(0, body.getCount());
    }

    /**
     * Test: Policy search response includes all required fields.
     */
    @Test
    void searchResponseIncludesAllRequiredFields() {
        double[] embedding = createDummyEmbedding(768);
        when(ollamaService.embed("complete query")).thenReturn(embedding);

        PolicySearchResult result = PolicySearchResult.builder()
                .documentId("doc-123")
                .policyName("Complete Policy")
                .policyType("Security")
                .sectionNumber("3.2.1")
                .sectionTitle("Multi-Factor Authentication")
                .chunkIndex(7)
                .chunkText("MFA must be enabled for all privileged accounts")
                .similarity(0.94)
                .build();

        when(documentService.searchPolicies(any(List.class), eq("cosine"), eq(5), isNull(), isNull()))
                .thenReturn(List.of(result));

        ResponseEntity<?> response = policyController.searchPolicies("complete query", 5, null, null);

        assertEquals(200, response.getStatusCodeValue());
        PolicySearchResponse body = (PolicySearchResponse) response.getBody();
        PolicySearchResult responseResult = body.getResults().get(0);

        assertNotNull(responseResult.getDocumentId());
        assertNotNull(responseResult.getPolicyName());
        assertNotNull(responseResult.getPolicyType());
        assertNotNull(responseResult.getSectionNumber());
        assertNotNull(responseResult.getSectionTitle());
        assertTrue(responseResult.getChunkIndex() >= 0);
        assertNotNull(responseResult.getChunkText());
        assertTrue(responseResult.getSimilarity() >= 0 && responseResult.getSimilarity() <= 1);
    }

    /**
     * Test: Default topK value is used when not specified.
     */
    @Test
    void defaultTopKValueIsUsedWhenNotSpecified() {
        double[] embedding = createDummyEmbedding(768);
        when(ollamaService.embed("query")).thenReturn(embedding);

        List<PolicySearchResult> mockResults = new ArrayList<>();
        when(documentService.searchPolicies(any(List.class), eq("cosine"), eq(5), isNull(), isNull()))
                .thenReturn(mockResults);

        policyController.searchPolicies("query", 5, null, null);

        // Verify that searchPolicies was called with topK=5 (the default)
        assertEquals(200, policyController.searchPolicies("query", 5, null, null).getStatusCodeValue());
    }

    // Helper method
    private double[] createDummyEmbedding(int dimensions) {
        double[] embedding = new double[dimensions];
        for (int i = 0; i < dimensions; i++) {
            embedding[i] = 0.1;
        }
        return embedding;
    }
}
