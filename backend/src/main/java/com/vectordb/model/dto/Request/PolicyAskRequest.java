package com.vectordb.model.dto.request;

import lombok.Data;

@Data
public class PolicyAskRequest {

    private String question;
    /** Maximum policy chunks to retrieve for context (default 5). */
    private int topK = 5;
}
