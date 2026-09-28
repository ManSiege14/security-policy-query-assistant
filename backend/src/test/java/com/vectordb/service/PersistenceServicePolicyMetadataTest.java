package com.vectordb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vectordb.model.DocItem;
import com.vectordb.model.VectorItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceServicePolicyMetadataTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setup() {
        mapper = new ObjectMapper();
    }

    @Test
    void docItemWithPolicyMetadataSerializesCorrectly(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("test_docs.json").toFile();

        Map<Integer, DocItem> data = new HashMap<>();
        data.put(1, DocItem.builder()
                .id(1)
                .documentId("doc-1")
                .chunkIndex(0)
                .title("Remote Access Policy [1/2]")
                .chunkText("All remote access...")
                .policyName("Remote Access Policy")
                .policyType("Access Control")
                .sectionNumber("3.1")
                .sectionTitle("VPN Requirements")
                .build());

        mapper.writerWithDefaultPrettyPrinter().writeValue(file, data);

        Map<Integer, DocItem> loaded = mapper.readValue(file,
                mapper.getTypeFactory().constructMapType(Map.class, Integer.class, DocItem.class));

        DocItem item = loaded.get(1);
        assertNotNull(item);
        assertEquals("Remote Access Policy", item.getPolicyName());
        assertEquals("Access Control", item.getPolicyType());
        assertEquals("3.1", item.getSectionNumber());
        assertEquals("VPN Requirements", item.getSectionTitle());
    }

    @Test
    void docItemWithoutPolicyMetadataDeserializesCorrectly(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("test_docs_old.json").toFile();

        // Simulate old format (no policy metadata fields)
        String json = "{ " +
                "\"1\": { " +
                "\"id\": 1, " +
                "\"documentId\": \"doc-1\", " +
                "\"chunkIndex\": 0, " +
                "\"title\": \"Old Document\", " +
                "\"chunkText\": \"Old content\" " +
                "} " +
                "}";

        mapper.writeValue(file, mapper.readValue(json, Object.class));

        Map<Integer, DocItem> loaded = mapper.readValue(file,
                mapper.getTypeFactory().constructMapType(Map.class, Integer.class, DocItem.class));

        DocItem item = loaded.get(1);
        assertNotNull(item);
        assertEquals("Old Document", item.getTitle());
        assertEquals("Old content", item.getChunkText());
        assertNull(item.getPolicyName());
        assertNull(item.getPolicyType());
        assertNull(item.getSectionNumber());
        assertNull(item.getSectionTitle());
    }

    @Test
    void mixedDocItemsWithAndWithoutMetadataDeserializeCorrectly(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("test_docs_mixed.json").toFile();

        Map<Integer, DocItem> data = new HashMap<>();
        
        // Document with policy metadata
        data.put(1, DocItem.builder()
                .id(1)
                .documentId("doc-1")
                .chunkIndex(0)
                .title("Policy Document [1/1]")
                .chunkText("Policy content")
                .policyName("Security Policy")
                .policyType("Access Control")
                .sectionNumber("2.1")
                .sectionTitle("Requirements")
                .build());

        // Document without policy metadata (backward compatibility)
        data.put(2, DocItem.builder()
                .id(2)
                .documentId("doc-2")
                .chunkIndex(0)
                .title("Regular Document")
                .chunkText("Regular content")
                .build());

        mapper.writerWithDefaultPrettyPrinter().writeValue(file, data);

        Map<Integer, DocItem> loaded = mapper.readValue(file,
                mapper.getTypeFactory().constructMapType(Map.class, Integer.class, DocItem.class));

        // Verify first item with metadata
        DocItem item1 = loaded.get(1);
        assertNotNull(item1);
        assertEquals("Security Policy", item1.getPolicyName());
        assertEquals("Access Control", item1.getPolicyType());

        // Verify second item without metadata
        DocItem item2 = loaded.get(2);
        assertNotNull(item2);
        assertEquals("Regular Document", item2.getTitle());
        assertNull(item2.getPolicyName());
        assertNull(item2.getPolicyType());
    }

    @Test
    void emptyJsonFileLoadsSuccessfully(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("empty.json").toFile();
        mapper.writeValue(file, new HashMap<>());

        Map<Integer, DocItem> loaded = mapper.readValue(file,
                mapper.getTypeFactory().constructMapType(Map.class, Integer.class, DocItem.class));

        assertTrue(loaded.isEmpty());
    }
}
