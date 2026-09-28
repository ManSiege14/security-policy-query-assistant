package com.vectordb.controller;

import com.vectordb.model.dto.request.PolicyAskRequest;
import com.vectordb.model.dto.response.PolicyAskResponse;
import com.vectordb.service.DocumentService;
import com.vectordb.service.OllamaService;
import com.vectordb.service.PdfService;
import com.vectordb.service.RagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PolicyAskControllerTest {

    private PolicyController policyController;

    @Mock
    private PdfService pdfService;

    @Mock
    private DocumentService documentService;

    @Mock
    private OllamaService ollamaService;

    @Mock
    private RagService ragService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        policyController = new PolicyController(pdfService, documentService, ollamaService, ragService);
    }

    @Test
    void askPolicyReturnsQuestionAndAnswer() {
        PolicyAskRequest request = new PolicyAskRequest();
        request.setQuestion("What are the password requirements?");
        request.setTopK(5);

        when(ragService.askPolicy(eq("What are the password requirements?"), eq(5)))
                .thenReturn(PolicyAskResponse.builder()
                        .question("What are the password requirements?")
                        .answer("Passwords must be changed every 90 days.")
                        .build());

        ResponseEntity<?> response = policyController.askPolicy(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        PolicyAskResponse body = (PolicyAskResponse) response.getBody();
        assertNotNull(body);
        assertEquals("What are the password requirements?", body.getQuestion());
        assertEquals("Passwords must be changed every 90 days.", body.getAnswer());
        verify(ragService).askPolicy("What are the password requirements?", 5);
    }

    @Test
    void askPolicyWithoutQuestionReturnsBadRequest() {
        PolicyAskRequest request = new PolicyAskRequest();
        request.setQuestion("   ");

        ResponseEntity<?> response = policyController.askPolicy(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(ragService);
    }
}
