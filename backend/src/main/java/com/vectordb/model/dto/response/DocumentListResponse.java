package com.vectordb.model.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentListResponse {

    private int id;
    private String documentId;
    private int chunkIndex;
    private String title;
    private String preview;   // Truncated chunk text for display
    private int wordCount;

    // ── Policy Metadata ────────────────────────────────────────────────────────
    private String policyName;     // e.g., "Remote Access Policy"
    private String policyType;     // e.g., "Access Control", "Data Protection"
    private String sectionNumber;  // e.g., "3.1"
    private String sectionTitle;   // e.g., "VPN Requirements"
}