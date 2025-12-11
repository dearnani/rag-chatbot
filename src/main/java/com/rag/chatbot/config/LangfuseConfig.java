package com.rag.chatbot.config;

import com.langfuse.client.LangfuseClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangfuseConfig {

    @Value("${langfuse.public-key}")
    private String publicKey;

    @Value("${langfuse.secret-key}")
    private String secretKey;

    @Value("${langfuse.host}")
    private String host;

    @Bean
    public LangfuseClient langfuseClient() {
        return LangfuseClient.builder()
                .credentials(publicKey, secretKey)
                .url(host)
                .build();
    }
}
