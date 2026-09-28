package com.vectordb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vectordb.model.dto.response.PolicyAskResponse;
import com.vectordb.model.dto.response.PolicySearchResult;
import com.vectordb.model.dto.response.PolicySourceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Stage 6 — sources on policy ask responses, mapped from retrieval results only.
 */
class PolicyAskSourcesTest {

    private RagService ragService;

    @Mock
    private OllamaService ollamaService;

    @Mock
    private DocumentService documentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ragService = new RagService(ollamaService, documentService);
    }

    @Test
    void policyAskReturnsSources() {
        stubRetrieval(twoOrderedChunks());
        when(ollamaService.generate(anyString())).thenReturn("Answer");

        PolicyAskResponse response = ragService.askPolicy("Question?", 5);

        assertNotNull(response.getSources());
        assertEquals(2, response.getSources().size());
    }

    @Test
    void sourcesMatchRetrievedPolicySearchResults() {
        List<PolicySearchResult> chunks = twoOrderedChunks();
        stubRetrieval(chunks);
        when(ollamaService.generate(anyString())).thenReturn("Answer");

        PolicyAskResponse response = ragService.askPolicy("Question?", 5);

        for (int i = 0; i < chunks.size(); i++) {
            PolicySearchResult expected = chunks.get(i);
            PolicySourceResponse source = response.getSources().get(i);
            assertEquals(expected.getPolicyName(), source.getPolicyName());
            assertEquals(expected.getPolicyType(), source.getPolicyType());
            assertEquals(expected.getSectionNumber(), source.getSectionNumber());
            assertEquals(expected.getSectionTitle(), source.getSectionTitle());
            assertEquals(expected.getChunkIndex(), source.getChunkIndex());
            assertEquals(expected.getSimilarity(), source.getSimilarity());
        }
    }

    @Test
    void sourceOrderingMatchesRetrievalOrdering() {
        List<PolicySearchResult> chunks = List.of(
                chunk("Policy A", "Type A", "1", "Intro", 0, 0.95),
                chunk("Policy B", "Type B", "2", "Rules", 1, 0.80),
                chunk("Policy C", "Type C", "3", "Appendix", 2, 0.70)
        );
        stubRetrieval(chunks);
        when(ollamaService.generate(anyString())).thenReturn("Answer");

        List<PolicySourceResponse> sources = ragService.askPolicy("Q?", 5).getSources();

        assertEquals("Policy A", sources.get(0).getPolicyName());
        assertEquals("Policy B", sources.get(1).getPolicyName());
        assertEquals("Policy C", sources.get(2).getPolicyName());
        assertEquals(0.95, sources.get(0).getSimilarity());
        assertEquals(0.80, sources.get(1).getSimilarity());
        assertEquals(0.70, sources.get(2).getSimilarity());
    }

    @Test
    void nullSectionMetadataPreservedInSources() {
        List<PolicySearchResult> chunks = List.of(
                PolicySearchResult.builder()
                        .policyName("General Policy")
                        .policyType("General")
                        .sectionNumber(null)
                        .sectionTitle(null)
                        .chunkIndex(4)
                        .chunkText("Content")
                        .similarity(0.87)
                        .build()
        );
        stubRetrieval(chunks);
        when(ollamaService.generate(anyString())).thenReturn("Answer");

        PolicySourceResponse source = ragService.askPolicy("Q?", 5).getSources().get(0);

        assertNull(source.getSectionNumber());
        assertNull(source.getSectionTitle());
        assertEquals(4, source.getChunkIndex());
        assertEquals(0.87, source.getSimilarity());
    }

    @Test
    void noRetrievalResultsReturnEmptySourcesAndSkipLlm() {
        when(ollamaService.embed(anyString())).thenReturn(createDummyEmbedding(768));
        when(documentService.searchPolicies(any(List.class), anyString(), anyInt()))
                .thenReturn(new ArrayList<>());

        PolicyAskResponse response = ragService.askPolicy("Nothing here?", 5);

        assertNotNull(response.getSources());
        assertTrue(response.getSources().isEmpty());
        verify(ollamaService, never()).generate(anyString());
    }

    @Test
    void sourcesDoNotExposeEmbeddingsOrChunkText() throws Exception {
        stubRetrieval(List.of(
                PolicySearchResult.builder()
                        .documentId("internal-doc-id")
                        .policyName("Access Policy")
                        .policyType("Access")
                        .chunkText("Secret chunk body")
                        .chunkIndex(0)
                        .similarity(0.9)
                        .build()
        ));
        when(ollamaService.generate(anyString())).thenReturn("Answer");

        PolicyAskResponse response = ragService.askPolicy("Q?", 5);
        String json = objectMapper.writeValueAsString(response);

        assertFalse(json.contains("Secret chunk body"));
        assertFalse(json.contains("embedding"));
        assertFalse(json.contains("internal-doc-id"));
        assertFalse(json.contains("documentId"));
        assertFalse(json.contains("chunkText"));
    }

    private List<PolicySearchResult> twoOrderedChunks() {
        return List.of(
                chunk("Password Security Policy", "Access Control", "3.2", "Password Expiry", 0, 0.92),
                chunk("Password Security Policy", "Access Control", "3.3", "Complexity", 1, 0.85)
        );
    }

    private PolicySearchResult chunk(
            String name, String type, String section, String title, int index, double similarity) {
        return PolicySearchResult.builder()
                .policyName(name)
                .policyType(type)
                .sectionNumber(section)
                .sectionTitle(title)
                .chunkIndex(index)
                .chunkText("Chunk " + index)
                .similarity(similarity)
                .build();
    }

    private void stubRetrieval(List<PolicySearchResult> chunks) {
        when(ollamaService.embed(anyString())).thenReturn(createDummyEmbedding(768));
        when(documentService.searchPolicies(any(List.class), anyString(), anyInt()))
                .thenReturn(chunks);
    }

    private double[] createDummyEmbedding(int dimensions) {
        double[] embedding = new double[dimensions];
        for (int i = 0; i < dimensions; i++) {
            embedding[i] = 0.1;
        }
        return embedding;
    }
}
