package com.vectordb.controller;

import com.vectordb.model.dto.response.StatusResponse;
import com.vectordb.service.DocumentService;
import com.vectordb.service.OllamaService;
import com.vectordb.service.VectorStoreService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    private final OllamaService ollamaService;
    private final DocumentService documentService;
    private final VectorStoreService demoStore;

    @Value("${vectordb.doc.dims}")
    private int docDims;

    public StatusController(
            OllamaService ollamaService,
            DocumentService documentService,
            @Qualifier("demoVectorStore") VectorStoreService demoStore) {

        this.ollamaService = ollamaService;
        this.documentService = documentService;
        this.demoStore = demoStore;
    }

    @GetMapping("/status")
    public StatusResponse status() {

        return StatusResponse.builder()
                .ollamaAvailable(ollamaService.isAvailable())
                .embedModel(ollamaService.getEmbedModel())
                .genModel(ollamaService.getGenModel())
                .docCount(documentService.size())
                .docDims(docDims)
                .demoCount(demoStore.size())
                .build();
    }
}