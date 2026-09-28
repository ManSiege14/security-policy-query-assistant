package com.vectordb.model.dto;

public class TextSearchRequest {
    private String query;
    private String metric = "cosine";
    private int k = 5;

    public String getQuery() { return query; }
    public String getMetric() { return metric; }
    public int getK() { return k; }

    public void setQuery(String query) { this.query = query; }
    public void setMetric(String metric) { this.metric = metric; }
    public void setK(int k) { this.k = k; }
}