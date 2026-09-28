package com.vectordb.model.dto.request;

import lombok.Data;

@Data
public class InsertDocumentRequest {

    private String title; // Document title supplied by client
    private String text;  // Full document body — will be chunked internally

    // ── Policy Metadata (optional) ──────────────────────────────────────────
    private String policyName;     // e.g., "Remote Access Policy"
    private String policyType;     // e.g., "Access Control", "Data Protection"
    private String sectionNumber;  // e.g., "3.1"
    private String sectionTitle;   // e.g., "VPN Requirements"
}