// src/main/java/com/vectordb/model/dto/response/DocumentSummaryResponse.java
package com.vectordb.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSummaryResponse {
    private String documentId;
    private String title;                      // Base title, chunk-index suffix stripped
    private int totalChunks;
    private List<DocumentListResponse> chunks;  // Full chunk details, reused from existing DTO
}