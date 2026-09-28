package com.vectordb.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response from a policy-aware search query.
 * Contains the original query, result count, and detailed policy search results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicySearchResponse {
    private String query;
    private List<PolicySearchResult> results;
    private int count;
}
