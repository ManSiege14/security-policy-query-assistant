// src/main/java/com/vectordb/model/dto/response/PolicyResponse.java
package com.vectordb.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a single policy document summary.
 * Includes policy metadata without detailed chunk content.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyResponse {
    
    private String documentId;   // Unique identifier for this policy document
    private String policyName;   // e.g., "Remote Access Policy"
    private String policyType;   // e.g., "Access Control"
    private int chunkCount;      // Number of chunks/sections in this policy
}
