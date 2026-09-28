package com.vectordb.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DocItemTest {

    @Test
    void docItemWithoutPolicyMetadataCreatedSuccessfully() {
        DocItem item = DocItem.builder()
                .id(1)
                .documentId("doc-123")
                .chunkIndex(0)
                .title("Sample Document")
                .chunkText("This is sample text")
                .build();

        assertEquals(1, item.getId());
        assertEquals("doc-123", item.getDocumentId());
        assertEquals(0, item.getChunkIndex());
        assertEquals("Sample Document", item.getTitle());
        assertEquals("This is sample text", item.getChunkText());
        assertNull(item.getPolicyName());
        assertNull(item.getPolicyType());
        assertNull(item.getSectionNumber());
        assertNull(item.getSectionTitle());
    }

    @Test
    void docItemWithPolicyMetadataCreatedSuccessfully() {
        DocItem item = DocItem.builder()
                .id(2)
                .documentId("doc-456")
                .chunkIndex(0)
                .title("Remote Access Policy [1/2]")
                .chunkText("All remote access requires VPN...")
                .policyName("Remote Access Policy")
                .policyType("Access Control")
                .sectionNumber("3.1")
                .sectionTitle("VPN Requirements")
                .build();

        assertEquals(2, item.getId());
        assertEquals("doc-456", item.getDocumentId());
        assertEquals("Remote Access Policy", item.getPolicyName());
        assertEquals("Access Control", item.getPolicyType());
        assertEquals("3.1", item.getSectionNumber());
        assertEquals("VPN Requirements", item.getSectionTitle());
    }

    @Test
    void docItemCanBeUpdatedWithPolicyMetadata() {
        DocItem item = DocItem.builder()
                .id(3)
                .documentId("doc-789")
                .chunkIndex(1)
                .title("Document Chunk [2/3]")
                .chunkText("Chunk content")
                .build();

        assertNull(item.getPolicyName());

        item.setPolicyName("Data Protection Policy");
        item.setPolicyType("Data Protection");
        item.setSectionNumber("2.1");
        item.setSectionTitle("Data Classification");

        assertEquals("Data Protection Policy", item.getPolicyName());
        assertEquals("Data Protection", item.getPolicyType());
        assertEquals("2.1", item.getSectionNumber());
        assertEquals("Data Classification", item.getSectionTitle());
    }
}
