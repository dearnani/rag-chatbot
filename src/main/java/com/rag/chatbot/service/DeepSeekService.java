package com.rag.chatbot.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for interacting with DeepSeek API
 */
@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);

    private final String apiKey;
    private String apiUrl;
    private final String model;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public DeepSeekService(com.rag.chatbot.config.DeepSeekProperties deepSeekProperties) {
        this.apiKey = deepSeekProperties.getApiKey();
        this.apiUrl = !deepSeekProperties.getApiUrl().isEmpty() ? deepSeekProperties.getApiUrl()
                : "http://localhost:8080/v1/chat/completions";
        this.model = deepSeekProperties.getModel();
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Generate a response using DeepSeek API
     */
    public String generateResponse(String userMessage, String context) {
        try {
            String systemPrompt = "You are a helpful assistant. Use the following context to answer the user's question. "
                    +
                    "If the context doesn't contain relevant information, say so.\n\nContext:\n" + context;

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system", systemPrompt));
            messages.add(new ChatMessage("user", userMessage));

            ChatRequest request = new ChatRequest(model, messages, 0.7, 1000);

            String requestBody = objectMapper.writeValueAsString(request);

            ChatResponse response = webClient.post()
                    .uri(apiUrl)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(ChatResponse.class)
                    .block();

            if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                return response.choices().get(0).message().content();
            }

            return "Sorry, I couldn't generate a response.";
        } catch (Exception e) {
            log.error("Failed to generate response from DeepSeek", e);
            throw new RuntimeException("Failed to generate response", e);
        }
    }

    record ChatRequest(
            String model,
            List<ChatMessage> messages,
            Double temperature,
            @JsonProperty("max_tokens") Integer maxTokens) {
    }

    record ChatMessage(String role, String content) {
    }

    record ChatResponse(List<Choice> choices) {
        record Choice(ChatMessage message) {
        }
    }
}
