package com.vectordb.controller;

import com.vectordb.core.VectorMath;
import com.vectordb.model.VectorItem;
import com.vectordb.model.dto.request.InsertVectorRequest;
import com.vectordb.model.dto.request.SearchRequest;
import com.vectordb.model.dto.response.SearchResponse;
import com.vectordb.model.dto.response.SearchResult;
import com.vectordb.service.VectorStoreService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final VectorStoreService demoStore;

    public DemoController(@Qualifier("demoVectorStore") VectorStoreService demoStore) {
        this.demoStore = demoStore;
    }

    @PostMapping("/insert")
    public ResponseEntity<Map<String, Object>> insert(@Valid @RequestBody InsertVectorRequest req) {
        int id = demoStore.insert(req.getMetadata(), req.getCategory(), req.getEmbedding());
        return ResponseEntity.ok(Map.of("id", id));
    }

    @GetMapping("/items")
    public ResponseEntity<List<VectorItem>> items() {
        return ResponseEntity.ok(demoStore.listAll());
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable int id) {
        boolean removed = demoStore.delete(id);
        return ResponseEntity.ok(Map.of("ok", removed));
    }

    @PostMapping("/search")
    public ResponseEntity<SearchResponse> search(@Valid @RequestBody SearchRequest req) {
        List<VectorItem> hits = demoStore.search(req.getEmbedding(), req.getK(), req.getMetric());

        List<SearchResult> results = hits.stream()
                .map(item -> SearchResult.builder()
                        .id(item.getId())
                        .metadata(item.getMetadata())
                        .category(item.getCategory())
                        .score(VectorMath.distance(
                                VectorMath.toArray(req.getEmbedding()),
                                VectorMath.toArray(item.getEmbedding()),
                                req.getMetric()
                        ))
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(SearchResponse.builder()
                .results(results)
                .resultCount(results.size())
                .build());
    }
}