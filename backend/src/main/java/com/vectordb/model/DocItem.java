// src/main/java/com/vectordb/model/DocItem.java
package com.vectordb.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocItem {

    private int id;            // Vector ID assigned by VectorStoreService
    private String documentId; // Groups all chunks from the same document
    private int chunkIndex;    // 0-based position of this chunk in the document
    private String title;      // Display title (includes chunk suffix if multi-chunk)
    private String chunkText;  // Raw chunk text — returned to RagService in Step 7

    // ── Policy Metadata ──────────────────────────────────────────────────────
    private String policyName;     // e.g., "Remote Access Policy"
    private String policyType;     // e.g., "Access Control", "Data Protection"
    private String sectionNumber;  // e.g., "3.1" (hierarchical section ID)
    private String sectionTitle;   // e.g., "VPN Requirements"
}