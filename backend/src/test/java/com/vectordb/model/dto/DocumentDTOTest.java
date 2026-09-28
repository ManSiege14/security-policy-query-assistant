package com.vectordb.model.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vectordb.model.dto.request.InsertDocumentRequest;
import com.vectordb.model.dto.response.DocumentListResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentDTOTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void insertDocumentRequestWithoutPolicyMetadata() throws Exception {
        String json = "{ \"title\": \"Test Doc\", \"text\": \"Some text\" }";
        
        InsertDocumentRequest request = mapper.readValue(json, InsertDocumentRequest.class);
        
        assertEquals("Test Doc", request.getTitle());
        assertEquals("Some text", request.getText());
        assertNull(request.getPolicyName());
        assertNull(request.getPolicyType());
        assertNull(request.getSectionNumber());
        assertNull(request.getSectionTitle());
    }

    @Test
    void insertDocumentRequestWithPolicyMetadata() throws Exception {
        String json = "{ " +
                "\"title\": \"Remote Access Policy\", " +
                "\"text\": \"Policy content...\", " +
                "\"policyName\": \"Remote Access Policy\", " +
                "\"policyType\": \"Access Control\", " +
                "\"sectionNumber\": \"3.1\", " +
                "\"sectionTitle\": \"VPN Requirements\" " +
                "}";
        
        InsertDocumentRequest request = mapper.readValue(json, InsertDocumentRequest.class);
        
        assertEquals("Remote Access Policy", request.getTitle());
        assertEquals("Remote Access Policy", request.getPolicyName());
        assertEquals("Access Control", request.getPolicyType());
        assertEquals("3.1", request.getSectionNumber());
        assertEquals("VPN Requirements", request.getSectionTitle());
    }

    @Test
    void documentListResponseSerializesWithPolicyMetadata() throws Exception {
        DocumentListResponse response = DocumentListResponse.builder()
                .id(1)
                .documentId("doc-123")
                .chunkIndex(0)
                .title("Remote Access Policy [1/2]")
                .preview("All remote access requires...")
                .wordCount(250)
                .policyName("Remote Access Policy")
                .policyType("Access Control")
                .sectionNumber("3.1")
                .sectionTitle("VPN Requirements")
                .build();
        
        String json = mapper.writeValueAsString(response);
        
        assertTrue(json.contains("\"policyName\":\"Remote Access Policy\""));
        assertTrue(json.contains("\"policyType\":\"Access Control\""));
        assertTrue(json.contains("\"sectionNumber\":\"3.1\""));
        assertTrue(json.contains("\"sectionTitle\":\"VPN Requirements\""));
    }

    @Test
    void documentListResponseDeserializesWithPolicyMetadata() throws Exception {
        String json = "{ " +
                "\"id\": 1, " +
                "\"documentId\": \"doc-123\", " +
                "\"chunkIndex\": 0, " +
                "\"title\": \"Test\", " +
                "\"preview\": \"preview\", " +
                "\"wordCount\": 10, " +
                "\"policyName\": \"Test Policy\", " +
                "\"policyType\": \"Test Type\", " +
                "\"sectionNumber\": \"1.1\", " +
                "\"sectionTitle\": \"Test Section\" " +
                "}";
        
        DocumentListResponse response = mapper.readValue(json, DocumentListResponse.class);
        
        assertEquals("Test Policy", response.getPolicyName());
        assertEquals("Test Type", response.getPolicyType());
        assertEquals("1.1", response.getSectionNumber());
        assertEquals("Test Section", response.getSectionTitle());
    }

    @Test
    void documentListResponseDeserializesWithoutPolicyMetadata() throws Exception {
        String json = "{ " +
                "\"id\": 1, " +
                "\"documentId\": \"doc-123\", " +
                "\"chunkIndex\": 0, " +
                "\"title\": \"Test\", " +
                "\"preview\": \"preview\", " +
                "\"wordCount\": 10 " +
                "}";
        
        DocumentListResponse response = mapper.readValue(json, DocumentListResponse.class);
        
        assertEquals(1, response.getId());
        assertNull(response.getPolicyName());
        assertNull(response.getPolicyType());
        assertNull(response.getSectionNumber());
        assertNull(response.getSectionTitle());
    }
}
