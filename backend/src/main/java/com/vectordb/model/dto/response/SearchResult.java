package com.vectordb.model.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SearchResult {
    int id;
    String metadata;
    String category;
    double score;
}