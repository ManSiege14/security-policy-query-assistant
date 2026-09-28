package com.vectordb.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorItem {

    private int id;
    private String metadata;
    private String category;
    private List<Double> embedding;
}