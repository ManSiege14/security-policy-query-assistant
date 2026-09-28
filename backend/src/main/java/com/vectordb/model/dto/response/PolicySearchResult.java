package com.vectordb.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single result from a policy-aware search query.
 * Includes policy metadata and similarity score for retrieval ranking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicySearchResult {
    private String documentId;
    private String policyName;
    private String policyType;
    private String sectionNumber;
    private String sectionTitle;
    private int chunkIndex;
    private String chunkText;
    private double similarity;
}
