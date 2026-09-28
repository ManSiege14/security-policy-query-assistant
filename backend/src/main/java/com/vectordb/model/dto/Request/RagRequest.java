package com.vectordb.model.dto.request;

import lombok.Data;

@Data
public class RagRequest {

    private String question;
    private int k = 3;
}