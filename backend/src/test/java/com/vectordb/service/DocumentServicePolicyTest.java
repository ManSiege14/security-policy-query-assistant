package com.vectordb.service;

import com.vectordb.model.DocItem;
import com.vectordb.model.dto.response.PolicyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for policy-specific methods in DocumentService.
 * Uses reflection to access and test the private metadataStore.
 */
@ExtendWith(MockitoExtension.class)
class DocumentServicePolicyTest {

    private DocumentService documentService;

    @Mock
    private OllamaService ollamaService;

    @Mock
    private VectorStoreService docStore;

    @Mock
    private PersistenceService persistenceService;

    private Map<Integer, DocItem> metadataStore;

    @BeforeEach
    void setup() throws Exception {
        documentService = new DocumentService(ollamaService, docStore, persistenceService);

        // Access private metadataStore via reflection for testing
        java.lang.reflect.Field field = DocumentService.class.getDeclaredField("metadataStore");
        field.setAccessible(true);
        metadataStore = (Map<Integer, DocItem>) field.get(documentService);
    }

    @Test
    void listPoliciesReturnsOnlyPoliciesWithMetadata() {
        // Add a policy chunk
        metadataStore.put(1, DocItem.builder()
                .id(1)
                .documentId("doc-1")
                .chunkIndex(0)
                .policyName("Remote Access Policy")
                .policyType("Access Control")
                .build());

        // Add another chunk from same policy
        metadataStore.put(2, DocItem.builder()
                .id(2)
                .documentId("doc-1")
                .chunkIndex(1)
                .policyName("Remote Access Policy")
                .policyType("Access Control")
                .build());

        // Add a generic document (no policy metadata)
        metadataStore.put(3, DocItem.builder()
                .id(3)
                .documentId("doc-2")
                .chunkIndex(0)
                .title("Generic Document")
                .chunkText("Content")
                .build());

        List<PolicyResponse> policies = documentService.listPolicies();

        // Should only return 1 policy (doc-1), not the generic document
        assertEquals(1, policies.size());
        assertEquals("doc-1", policies.get(0).getDocumentId());
        assertEquals("Remote Access Policy", policies.get(0).getPolicyName());
        assertEquals(2, policies.get(0).getChunkCount());  // Two chunks in the policy
    }

    @Test
    void listPoliciesReturnMultiplePolicies() {
        // Policy 1: 3 chunks
        for (int i = 1; i <= 3; i++) {
            metadataStore.put(i, DocItem.builder()
                    .id(i)
                    .documentId("doc-1")
                    .chunkIndex(i - 1)
                    .policyName("Remote Access Policy")
                    .policyType("Access Control")
                    .build());
        }

        // Policy 2: 2 chunks
        for (int i = 4; i <= 5; i++) {
            metadataStore.put(i, DocItem.builder()
                    .id(i)
                    .documentId("doc-2")
                    .chunkIndex(i - 4)
                    .policyName("Data Protection Policy")
                    .policyType("Data Protection")
                    .build());
        }

        List<PolicyResponse> policies = documentService.listPolicies();

        assertEquals(2, policies.size());

        // Verify policy 1
        PolicyResponse policy1 = policies.stream()
                .filter(p -> p.getDocumentId().equals("doc-1"))
                .findFirst()
                .orElse(null);
        assertNotNull(policy1);
        assertEquals("Remote Access Policy", policy1.getPolicyName());
        assertEquals(3, policy1.getChunkCount());

        // Verify policy 2
        PolicyResponse policy2 = policies.stream()
                .filter(p -> p.getDocumentId().equals("doc-2"))
                .findFirst()
                .orElse(null);
        assertNotNull(policy2);
        assertEquals("Data Protection Policy", policy2.getPolicyName());
        assertEquals(2, policy2.getChunkCount());
    }

    @Test
    void listPoliciesWithEmptyMetadataStoreReturnsEmpty() {
        List<PolicyResponse> policies = documentService.listPolicies();
        assertTrue(policies.isEmpty());
    }

    @Test
    void getPolicyDetailReturnsCorrectPolicy() {
        metadataStore.put(1, DocItem.builder()
                .id(1)
                .documentId("doc-123")
                .chunkIndex(0)
                .policyName("Remote Access Policy")
                .policyType("Access Control")
                .build());

        metadataStore.put(2, DocItem.builder()
                .id(2)
                .documentId("doc-123")
                .chunkIndex(1)
                .policyName("Remote Access Policy")
                .policyType("Access Control")
                .build());

        Optional<PolicyResponse> result = documentService.getPolicyDetail("doc-123");

        assertTrue(result.isPresent());
        PolicyResponse policy = result.get();
        assertEquals("doc-123", policy.getDocumentId());
        assertEquals("Remote Access Policy", policy.getPolicyName());
        assertEquals("Access Control", policy.getPolicyType());
        assertEquals(2, policy.getChunkCount());
    }

    @Test
    void getPolicyDetailWithNonexistentDocumentReturnsEmpty() {
        Optional<PolicyResponse> result = documentService.getPolicyDetail("nonexistent");
        assertFalse(result.isPresent());
    }

    @Test
    void getPolicyDetailUsesFirstChunkMetadata() {
        // Add chunks with same policy name/type but different chunk content
        metadataStore.put(1, DocItem.builder()
                .id(1)
                .documentId("doc-123")
                .chunkIndex(0)
                .chunkText("Chunk 1 content")
                .policyName("Test Policy")
                .policyType("Test Type")
                .build());

        metadataStore.put(2, DocItem.builder()
                .id(2)
                .documentId("doc-123")
                .chunkIndex(1)
                .chunkText("Chunk 2 content")
                .policyName("Test Policy")
                .policyType("Test Type")
                .build());

        Optional<PolicyResponse> result = documentService.getPolicyDetail("doc-123");

        assertTrue(result.isPresent());
        PolicyResponse policy = result.get();
        // Should use metadata from first chunk
        assertEquals("Test Policy", policy.getPolicyName());
        assertEquals("Test Type", policy.getPolicyType());
        // Should count all chunks
        assertEquals(2, policy.getChunkCount());
    }

    @Test
    void listPoliciesIgnoresDocumentsWithoutPolicyName() {
        // Document with policy metadata
        metadataStore.put(1, DocItem.builder()
                .id(1)
                .documentId("doc-1")
                .chunkIndex(0)
                .policyName("Valid Policy")
                .policyType("Type")
                .build());

        // Document without policy metadata (policyName is null)
        metadataStore.put(2, DocItem.builder()
                .id(2)
                .documentId("doc-2")
                .chunkIndex(0)
                .chunkText("No policy metadata")
                .build());

        // Document without policyType
        metadataStore.put(3, DocItem.builder()
                .id(3)
                .documentId("doc-3")
                .chunkIndex(0)
                .policyName("Incomplete Policy")
                .build());

        List<PolicyResponse> policies = documentService.listPolicies();

        // Should only include doc-1 which has both policyName and policyType
        assertEquals(1, policies.size());
        assertEquals("doc-1", policies.get(0).getDocumentId());
    }

    @Test
    void listPoliciesPreservesInsertionOrder() {
        // Insert policies in specific order
        metadataStore.put(1, DocItem.builder()
                .id(1)
                .documentId("doc-1")
                .policyName("Alpha Policy")
                .policyType("Type A")
                .build());

        metadataStore.put(3, DocItem.builder()
                .id(3)
                .documentId("doc-2")
                .policyName("Beta Policy")
                .policyType("Type B")
                .build());

        metadataStore.put(2, DocItem.builder()
                .id(2)
                .documentId("doc-1")
                .chunkIndex(1)
                .policyName("Alpha Policy")
                .policyType("Type A")
                .build());

        List<PolicyResponse> policies = documentService.listPolicies();

        // Should preserve order by vectorId (1, 2, 3)
        assertEquals(2, policies.size());
        assertEquals("doc-1", policies.get(0).getDocumentId());
        assertEquals("doc-2", policies.get(1).getDocumentId());
    }
}
