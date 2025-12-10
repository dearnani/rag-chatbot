package com.rag.chatbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "embedding")
record EmbeddingProperties(String modelName, String modelPath) {
    public EmbeddingProperties {
        if (modelName == null) {
            modelName = "BAAI/bge-small-en-v1.5";
        }
        if (modelPath == null) {
            modelPath = "./models";
        }
    }
}
