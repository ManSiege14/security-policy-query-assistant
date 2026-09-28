package com.vectordb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vectordb.model.DocItem;
import com.vectordb.model.VectorItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;

@Slf4j
@Service
public class PersistenceService {

    private final ObjectMapper mapper = new ObjectMapper();
    private static final String DATA_DIR = "data";
    private static final String VECTORS_FILE = "data/vectors.json";
    private static final String DOCUMENTS_FILE = "data/documents.json";

    /**
     * Atomically saves both vector store data and metadata store chunks.
     */
    public void saveAll(Map<Integer, VectorItem> vectorStore, Map<Integer, DocItem> metadataStore) throws Exception {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Save vectors
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(VECTORS_FILE), vectorStore);
        
        // Save text metadata
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(DOCUMENTS_FILE), metadataStore);
        
        log.info("Successfully persisted application state to disk.");
    }

    public Map<Integer, VectorItem> loadVectors() throws Exception {
        File file = new File(VECTORS_FILE);
        if (!file.exists()) return Map.of();

        return mapper.readValue(file, mapper.getTypeFactory()
                .constructMapType(Map.class, Integer.class, VectorItem.class));
    }

    public Map<Integer, DocItem> loadDocuments() throws Exception {
        File file = new File(DOCUMENTS_FILE);
        if (!file.exists()) return Map.of();

        return mapper.readValue(file, mapper.getTypeFactory()
                .constructMapType(Map.class, Integer.class, DocItem.class));
    }
}