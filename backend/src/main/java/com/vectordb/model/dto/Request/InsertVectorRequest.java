package com.vectordb.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class InsertVectorRequest {

    @NotBlank(message = "metadata is required")
    private String metadata;

    @NotBlank(message = "category is required")
    private String category;

    @NotEmpty(message = "embedding is required")
    private List<Double> embedding;
}