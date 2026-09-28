package com.vectordb.controller;

import com.vectordb.model.dto.request.RagRequest;
import com.vectordb.model.dto.response.RagResponse;
import com.vectordb.service.RagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    public ResponseEntity<RagResponse> ask(@RequestBody RagRequest request) {
        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(ragService.ask(request));
    }
}