package com.vectordb.model.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SearchResponse {
    List<SearchResult> results;
    int resultCount;
}