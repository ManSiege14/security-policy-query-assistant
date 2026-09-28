package com.vectordb.controller;

import com.vectordb.model.dto.response.PolicyResponse;
import com.vectordb.service.DocumentService;
import com.vectordb.service.PdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyControllerTest {

    @Mock
    private PdfService pdfService;

    @Mock
    private DocumentService documentService;

    @Mock
    private MultipartFile mockFile;

    @InjectMocks
    private PolicyController policyController;

    @Test
    void uploadPolicyWithValidDataReturnsSuccess() throws Exception {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test-policy.pdf");
        when(pdfService.extractText(mockFile)).thenReturn("Policy content...");
        when(documentService.insertDocument(any())).thenReturn(List.of(1, 2, 3));
        when(documentService.getDocItem(1)).thenReturn(Optional.of(
                com.vectordb.model.DocItem.builder()
                        .id(1)
                        .documentId("doc-123")
                        .build()
        ));

        ResponseEntity<?> response = policyController.uploadPolicy(
                mockFile,
                "Remote Access Policy",
                "Access Control",
                null,
                null
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof java.util.Map);

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> body = (java.util.Map<String, Object>) response.getBody();
        assertEquals(true, body.get("success"));
        assertEquals(3, body.get("chunks"));
        assertEquals("doc-123", body.get("documentId"));
    }

    @Test
    void uploadPolicyWithoutFileReturnsBadRequest() {
        ResponseEntity<?> response = policyController.uploadPolicy(
                null,
                "Policy Name",
                "Policy Type",
                null,
                null
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void uploadPolicyWithEmptyFileReturnsBadRequest() {
        when(mockFile.isEmpty()).thenReturn(true);

        ResponseEntity<?> response = policyController.uploadPolicy(
                mockFile,
                "Policy Name",
                "Policy Type",
                null,
                null
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void uploadPolicyWithoutPolicyNameReturnsBadRequest() throws Exception {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test-policy.pdf");

        ResponseEntity<?> response = policyController.uploadPolicy(
                mockFile,
                "",  // Empty policy name
                "Policy Type",
                null,
                null
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void uploadPolicyWithoutPolicyTypeReturnsBadRequest() throws Exception {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test-policy.pdf");

        ResponseEntity<?> response = policyController.uploadPolicy(
                mockFile,
                "Policy Name",
                null,  // Null policy type
                null,
                null
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void uploadPolicyWithNonPdfFileReturnsBadRequest() {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("policy.txt");

        ResponseEntity<?> response = policyController.uploadPolicy(
                mockFile,
                "Policy Name",
                "Policy Type",
                null,
                null
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void uploadPolicyWithBlankExtractedTextReturnsBadRequest() throws Exception {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test-policy.pdf");
        when(pdfService.extractText(mockFile)).thenReturn("");

        ResponseEntity<?> response = policyController.uploadPolicy(
                mockFile,
                "Policy Name",
                "Policy Type",
                null,
                null
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void uploadPolicyWhenOllamaUnavailableReturns503() throws Exception {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test-policy.pdf");
        when(pdfService.extractText(mockFile)).thenReturn("Policy content...");
        when(documentService.insertDocument(any())).thenReturn(List.of());  // Empty = Ollama failure

        ResponseEntity<?> response = policyController.uploadPolicy(
                mockFile,
                "Policy Name",
                "Policy Type",
                null,
                null
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void listPoliciesReturnsOkWithPolicies() {
        List<PolicyResponse> policies = List.of(
                PolicyResponse.builder()
                        .documentId("doc-1")
                        .policyName("Policy 1")
                        .policyType("Type 1")
                        .chunkCount(5)
                        .build()
        );
        when(documentService.listPolicies()).thenReturn(policies);

        ResponseEntity<?> response = policyController.listPolicies();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void getPolicyDetailWithValidDocumentIdReturnsPolicy() {
        PolicyResponse policy = PolicyResponse.builder()
                .documentId("doc-123")
                .policyName("Test Policy")
                .policyType("Test Type")
                .chunkCount(5)
                .build();
        when(documentService.getPolicyDetail("doc-123")).thenReturn(Optional.of(policy));

        ResponseEntity<?> response = policyController.getPolicyDetail("doc-123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getPolicyDetailWithInvalidDocumentIdReturnsNotFound() {
        when(documentService.getPolicyDetail("invalid")).thenReturn(Optional.empty());

        ResponseEntity<?> response = policyController.getPolicyDetail("invalid");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deletePolicyWithValidDocumentIdReturnsSuccess() {
        when(documentService.deleteDocumentGroup("doc-123")).thenReturn(5);

        ResponseEntity<?> response = policyController.deletePolicy("doc-123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> body = (java.util.Map<String, Object>) response.getBody();
        assertEquals(true, body.get("success"));
        assertEquals(5, body.get("chunksRemoved"));
    }

    @Test
    void deletePolicyWithInvalidDocumentIdReturnsNotFound() {
        when(documentService.deleteDocumentGroup("invalid")).thenReturn(0);

        ResponseEntity<?> response = policyController.deletePolicy("invalid");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
