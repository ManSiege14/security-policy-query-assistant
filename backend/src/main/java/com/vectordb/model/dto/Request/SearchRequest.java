package com.vectordb.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class SearchRequest {

    @NotEmpty(message = "embedding is required")
    private List<Double> embedding;

    @Min(value = 1, message = "k must be at least 1")
    private int k = 5;

    @Pattern(regexp = "cosine|euclidean|manhattan", message = "metric must be cosine, euclidean, or manhattan")
    private String metric = "cosine";
}