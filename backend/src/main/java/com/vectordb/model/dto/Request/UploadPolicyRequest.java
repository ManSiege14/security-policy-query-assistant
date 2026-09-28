// src/main/java/com/vectordb/model/dto/request/UploadPolicyRequest.java
package com.vectordb.model.dto.request;

import lombok.Data;

/**
 * Request DTO for uploading a security policy PDF.
 * policyName and policyType are required.
 * sectionNumber and sectionTitle are optional (future use).
 */
@Data
public class UploadPolicyRequest {
    
    private String policyName;     // Required: e.g., "Remote Access Policy"
    private String policyType;     // Required: e.g., "Access Control"
    private String sectionNumber;  // Optional: section identifier
    private String sectionTitle;   // Optional: section display name
}
