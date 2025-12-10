package com.rag.chatbot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "qdrant")
@Getter
@Setter
public class QdrantProperties {
    private String host = "localhost";
    private int port = 6333;
    private String collectionName = "rag-documents";
    private int vectorSize = 384;
    
    // Explicit getters for Lombok compatibility
    public String getHost() {
        return host;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getCollectionName() {
        return collectionName;
    }
    
    public int getVectorSize() {
        return vectorSize;
    }
}

