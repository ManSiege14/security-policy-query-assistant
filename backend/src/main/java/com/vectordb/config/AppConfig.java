package com.vectordb.config;

import com.vectordb.service.VectorStoreService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Value("${vectordb.demo.dims:16}")
    private int demoDims;

    @Value("${vectordb.doc.dims:768}")
    private int docDims;

    @Bean(name = "demoVectorStore")
    public VectorStoreService demoVectorStore() {
        return new VectorStoreService(demoDims);
    }

    @Bean(name = "docVectorStore")
    public VectorStoreService docVectorStore() {
        return new VectorStoreService(docDims);
    }
}