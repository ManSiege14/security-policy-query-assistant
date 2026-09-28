// src/main/java/com/vectordb/model/dto/response/StatusResponse.java
package com.vectordb.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StatusResponse {

    @JsonProperty("ollamaAvailable")
    boolean ollamaAvailable;

    @JsonProperty("embedModel")
    String embedModel;

    @JsonProperty("genModel")
    String genModel;

    @JsonProperty("docCount")
    int docCount;

    @JsonProperty("docDims")
    int docDims;

    @JsonProperty("demoCount")
    int demoCount;
}