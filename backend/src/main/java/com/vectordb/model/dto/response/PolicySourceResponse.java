package com.vectordb.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Client-facing citation for a policy chunk used as RAG context.
 * Derived from retrieval results only — not from LLM output.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicySourceResponse {

    private String policyName;
    private String policyType;
    private String sectionNumber;
    private String sectionTitle;
    private int chunkIndex;
    private double similarity;
}
