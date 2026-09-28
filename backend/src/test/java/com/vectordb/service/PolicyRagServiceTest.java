package com.vectordb.service;

import com.vectordb.model.DocItem;
import com.vectordb.model.VectorItem;
import com.vectordb.model.dto.request.RagRequest;
import com.vectordb.model.dto.response.PolicyAskResponse;
import com.vectordb.model.dto.response.PolicySearchResult;
import com.vectordb.model.dto.response.RagResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PolicyRagServiceTest {

    private RagService ragService;

    @Mock
    private OllamaService ollamaService;

    @Mock
    private DocumentService documentService;

    @Mock
    private VectorStoreService docStore;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ragService = new RagService(ollamaService, documentService);
    }

    @Test
    void policyQuestionRetrievesPolicyContextAndCallsLlm() {
        double[] embedding = createDummyEmbedding(768);
        when(ollamaService.embed("What are the password requirements?")).thenReturn(embedding);

        List<PolicySearchResult> chunks = List.of(
                PolicySearchResult.builder()
                        .documentId("doc-001")
                        .policyName("Password Security Policy")
                        .policyType("Access Control")
                        .sectionNumber("3.2")
                        .sectionTitle("Password Expiry")
                        .chunkIndex(0)
                        .chunkText("Passwords must be changed every 90 days.")
                        .similarity(0.92)
                        .build()
        );
        when(documentService.searchPolicies(any(List.class), eq("cosine"), eq(5)))
                .thenReturn(chunks);
        when(ollamaService.generate(anyString())).thenReturn("Passwords must be changed every 90 days.");

        PolicyAskResponse response = ragService.askPolicy("What are the password requirements?", 5);

        assertEquals("What are the password requirements?", response.getQuestion());
        assertEquals("Passwords must be changed every 90 days.", response.getAnswer());

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(ollamaService).generate(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("Password Security Policy"));
        assertTrue(prompt.contains("Passwords must be changed every 90 days."));
        assertTrue(prompt.contains("--- BEGIN POLICY CONTEXT ---"));
        assertTrue(prompt.contains("--- END POLICY CONTEXT ---"));
    }

    @Test
    void promptInstructsModelNotToInventPolicyInformation() {
        when(ollamaService.embed(anyString())).thenReturn(createDummyEmbedding(768));
        when(documentService.searchPolicies(any(List.class), anyString(), anyInt()))
                .thenReturn(List.of(samplePolicyChunk()));
        when(ollamaService.generate(anyString())).thenReturn("Answer");

        ragService.askPolicy("Question?", 3);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(ollamaService).generate(promptCaptor.capture());
        String prompt = promptCaptor.getValue();

        assertTrue(prompt.contains("using ONLY the policy information"));
        assertTrue(prompt.contains("Do not invent, assume, or infer organizational rules"));
        assertTrue(prompt.contains("Do not present general knowledge as an organizational policy"));
    }

    @Test
    void policyMetadataAppearsInGeneratedContext() {
        when(ollamaService.embed(anyString())).thenReturn(createDummyEmbedding(768));
        when(documentService.searchPolicies(any(List.class), anyString(), anyInt()))
                .thenReturn(List.of(
                        PolicySearchResult.builder()
                                .policyName("Data Protection Policy")
                                .policyType("Data Security")
                                .sectionNumber("2.1")
                                .sectionTitle("Encryption")
                                .chunkText("Data at rest must be encrypted.")
                                .build()
                ));
        when(ollamaService.generate(anyString())).thenReturn("Encrypted at rest.");

        ragService.askPolicy("Encryption rules?", 5);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(ollamaService).generate(promptCaptor.capture());
        String prompt = promptCaptor.getValue();

        assertTrue(prompt.contains("Policy: Data Protection Policy"));
        assertTrue(prompt.contains("Policy Type: Data Security"));
        assertTrue(prompt.contains("Section: 2.1"));
        assertTrue(prompt.contains("Section Title: Encryption"));
    }

    @Test
    void missingSectionMetadataDoesNotCauseFailure() {
        when(ollamaService.embed(anyString())).thenReturn(createDummyEmbedding(768));
        when(documentService.searchPolicies(any(List.class), anyString(), anyInt()))
                .thenReturn(List.of(
                        PolicySearchResult.builder()
                                .policyName("General Policy")
                                .policyType("General")
                                .sectionNumber(null)
                                .sectionTitle(null)
                                .chunkText("All users must comply.")
                                .build()
                ));
        when(ollamaService.generate(anyString())).thenReturn("Users must comply.");

        PolicyAskResponse response = ragService.askPolicy("Compliance?", 5);

        assertNotNull(response.getAnswer());
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(ollamaService).generate(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertFalse(prompt.contains("Section:\n"));
        assertFalse(prompt.contains("Section Title:\n"));
        assertTrue(prompt.contains("All users must comply."));
    }

    @Test
    void noRetrievalResultsDoNotCallLlm() {
        when(ollamaService.embed(anyString())).thenReturn(createDummyEmbedding(768));
        when(documentService.searchPolicies(any(List.class), anyString(), anyInt()))
                .thenReturn(new ArrayList<>());

        PolicyAskResponse response = ragService.askPolicy("Unknown topic?", 5);

        verify(ollamaService, never()).generate(anyString());
        assertEquals(RagService.NO_POLICY_CONTEXT_ANSWER, response.getAnswer());
    }

    @Test
    void noRetrievalResultsReturnControlledResponse() {
        when(ollamaService.embed("Any question")).thenReturn(createDummyEmbedding(768));
        when(documentService.searchPolicies(any(List.class), eq("cosine"), eq(5)))
                .thenReturn(List.of());

        PolicyAskResponse response = ragService.askPolicy("Any question", 5);

        assertEquals("Any question", response.getQuestion());
        assertTrue(response.getAnswer().toLowerCase().contains("could not find relevant information"));
    }

    @Test
    void genericRagStillUsesDocStoreSearch() {
        when(ollamaService.embed("Generic question")).thenReturn(createDummyEmbedding(768));
        when(documentService.getDocStore()).thenReturn(docStore);
        when(docStore.search(any(List.class), eq(3), eq("cosine")))
                .thenReturn(List.of(VectorItem.builder().id(1).embedding(List.of(0.1)).build()));
        when(documentService.getDocItem(1)).thenReturn(Optional.of(
                DocItem.builder()
                        .id(1)
                        .title("Generic Doc")
                        .chunkText("Generic content")
                        .build()
        ));
        when(ollamaService.generate(anyString())).thenReturn("Generic answer");

        RagRequest request = new RagRequest();
        request.setQuestion("Generic question");
        request.setK(3);

        RagResponse response = ragService.ask(request);

        assertEquals("Generic answer", response.getAnswer());
        assertFalse(response.getContext().isEmpty());
        verify(documentService).getDocStore();
        verify(docStore).search(any(List.class), eq(3), eq("cosine"));
        verify(documentService, never()).searchPolicies(any(), anyString(), anyInt());
    }

    @Test
    void buildPolicyPromptDoesNotIncludeEmbeddingsOrObjectReferences() {
        String prompt = ragService.buildPolicyPrompt(
                "Test?",
                List.of(samplePolicyChunk()));

        assertFalse(prompt.contains("@"));
        assertFalse(prompt.contains("embedding"));
        assertFalse(prompt.contains("similarity"));
    }

    private PolicySearchResult samplePolicyChunk() {
        return PolicySearchResult.builder()
                .documentId("doc-001")
                .policyName("Access Policy")
                .policyType("Access")
                .chunkText("Remote access requires MFA.")
                .similarity(0.88)
                .build();
    }

    private double[] createDummyEmbedding(int dimensions) {
        double[] embedding = new double[dimensions];
        for (int i = 0; i < dimensions; i++) {
            embedding[i] = 0.1;
        }
        return embedding;
    }
}
