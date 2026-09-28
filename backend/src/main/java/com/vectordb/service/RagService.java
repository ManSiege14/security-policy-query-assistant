package com.vectordb.service;

import com.vectordb.model.DocItem;
import com.vectordb.model.dto.request.RagRequest;
import com.vectordb.model.dto.response.RagResponse;
import com.vectordb.model.VectorItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class RagService {

    private final OllamaService ollamaService;
    private final DocumentService documentService;

    public RagService(OllamaService ollamaService, DocumentService documentService) {
        this.ollamaService = ollamaService;
        this.documentService = documentService;
    }

    public RagResponse ask(RagRequest request) {
        String question = request.getQuestion();
        int k = request.getK() > 0 ? request.getK() : 3;

        // Step 1: Embed the question
        double[] raw = ollamaService.embed(question);
        if (raw.length == 0) {
            log.warn("Embedding failed for question: '{}'", question);
            return RagResponse.builder()
                    .question(question)
                    .answer("Embedding service unavailable. Please check Ollama.")
                    .context(List.of())
                    .build();
        }

        List<Double> queryEmbedding = new ArrayList<>(raw.length);
        for (double v : raw) {
            queryEmbedding.add(v);
        }

        // Step 2: Search for top-K similar chunks
        List<VectorItem> results = documentService
        .getDocStore()
        .search(queryEmbedding, k, "cosine");

        log.info("Retrieved {} chunk(s) for question: '{}'", results.size(), question);

        // Step 3: Resolve vector IDs to DocItem metadata
        List<DocItem> context = new ArrayList<>();
        for (VectorItem result : results) {
            documentService.getDocItem(result.getId())
                    .ifPresent(context::add);
        }

        // Step 4: Build prompt
        String prompt = buildPrompt(question, context);
        log.debug("Prompt:\n{}", prompt);

        // Step 5: Generate answer
        String answer = ollamaService.generate(prompt);

        return RagResponse.builder()
                .question(question)
                .answer(answer)
                .context(context)
                .build();
    }

    private String buildPrompt(String question, List<DocItem> context) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are a helpful assistant. Answer the user's question using only the context below.\n");
        sb.append("If the context does not contain enough information, say so clearly.\n\n");

        sb.append("Context:\n");
        for (int i = 0; i < context.size(); i++) {
            sb.append("[").append(i + 1).append("] ")
              .append(context.get(i).getTitle()).append(":\n")
              .append(context.get(i).getChunkText()).append("\n\n");
        }

        sb.append("Question: ").append(question).append("\n\n");
        sb.append("Answer:");

        return sb.toString();
    }
}