package com.vectordb.model.dto.response;

import com.vectordb.model.DocItem;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RagResponse {

    private String question;
    private String answer;
    private List<DocItem> context;
}