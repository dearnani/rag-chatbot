package com.rag.chatbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class QdrantConfig {

    private final QdrantProperties qdrantProperties;

    public QdrantConfig(QdrantProperties qdrantProperties) {
        this.qdrantProperties = qdrantProperties;
    }

    // @SuppressWarnings(value = { "" })
    @Bean
    public WebClient qdrantWebClient() {
        String host = java.util.Objects.requireNonNull(qdrantProperties.getHost(), "Qdrant host must not be null");
        int port = qdrantProperties.getPort();
        return WebClient.builder()
                .baseUrl(String.format("http://%s:%d", host, port))
                .build();
    }
}
