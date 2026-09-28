package com.vectordb.service;

import com.vectordb.core.VectorMath;
import com.vectordb.model.VectorItem;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class VectorStoreService {

    private final Map<Integer, VectorItem> store = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);
    private final int expectedDims;

    public VectorStoreService(int expectedDims) {
        this.expectedDims = expectedDims;
    }

    public int insert(String metadata, String category, List<Double> embedding) {
        validateDimensions(embedding);
        int id = idCounter.getAndIncrement();
        VectorItem item = VectorItem.builder()
                .id(id)
                .metadata(metadata)
                .category(category)
                .embedding(embedding)
                .build();
        store.put(id, item);
        return id;
    }

    public List<VectorItem> search(List<Double> query, int k, String metric) {
    validateDimensions(query);

    double[] queryArr = VectorMath.toArray(query);

    return store.values().stream()
            .sorted(Comparator.comparingDouble(item ->
                    VectorMath.distance(
                            queryArr,
                            VectorMath.toArray(item.getEmbedding()),
                            metric
                    )))
            .limit(k)
            .collect(Collectors.toList());
}

    public boolean delete(int id) {
        return store.remove(id) != null;
    }

    public List<VectorItem> listAll() {
        return new ArrayList<>(store.values());
    }

    public int size() {
        return store.size();
    }
    public Map<Integer, VectorItem> getStore() {
    return store;
}

public void loadStore(
        Map<Integer, VectorItem> data
) {
    store.clear();
    store.putAll(data);

    int maxId = data.keySet()
            .stream()
            .max(Integer::compareTo)
            .orElse(0);

    idCounter.set(maxId + 1);
}
    private void validateDimensions(List<Double> embedding) {
        if (embedding == null || embedding.size() != expectedDims) {
            throw new IllegalArgumentException(
                    "Expected " + expectedDims + "D vector, got " +
                    (embedding == null ? "null" : embedding.size()) + "D");
        }
    }
}