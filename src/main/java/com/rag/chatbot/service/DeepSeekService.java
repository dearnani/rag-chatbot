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

    private final com.langfuse.client.LangfuseClient langfuseClient;

    public DeepSeekService(com.rag.chatbot.config.DeepSeekProperties deepSeekProperties,
            com.langfuse.client.LangfuseClient langfuseClient) {
        this.apiKey = deepSeekProperties.getApiKey();
        this.apiUrl = !deepSeekProperties.getApiUrl().isEmpty() ? deepSeekProperties.getApiUrl()
                : "http://localhost:8080/v1/chat/completions";
        this.model = deepSeekProperties.getModel();
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.langfuseClient = langfuseClient;
    }

    /**
     * Generate a response using DeepSeek API
     */
    public String generateResponse(String userMessage, String context) {
        long startTime = System.currentTimeMillis();
        String systemPrompt = "You are a helpful assistant. Use the following context to answer the user's question. "
                +
                "If the context doesn't contain relevant information, say so.\n\nContext:\n" + context;

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", systemPrompt));
        messages.add(new ChatMessage("user", userMessage));

        ChatRequest request = new ChatRequest(model, messages, 0.7, 1000);

        String result = "Sorry, I couldn't generate a response.";
        String metadata = null;

        try {
            String requestBody = objectMapper.writeValueAsString(request);

            ChatResponse response = webClient.post()
                    .uri(apiUrl)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(ChatResponse.class)
                    .block();

            if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                result = response.choices().get(0).message().content();
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to generate response from DeepSeek", e);
            metadata = e.getMessage();
            throw new RuntimeException("Failed to generate response", e);
        } finally {
            try {
                // Trace generation in Langfuse
                com.langfuse.client.resources.ingestion.types.CreateGenerationBody body = com.langfuse.client.resources.ingestion.types.CreateGenerationBody
                        .builder()
                        .name("deepseek-generation")
                        .model(model)
                        .input(messages)
                        .output(result)
                        .startTime(java.time.Instant.ofEpochMilli(startTime).atOffset(java.time.ZoneOffset.UTC))
                        .endTime(java.time.Instant.now().atOffset(java.time.ZoneOffset.UTC))
                        .build();

                com.langfuse.client.resources.ingestion.types.CreateGenerationEvent event = com.langfuse.client.resources.ingestion.types.CreateGenerationEvent
                        .builder()
                        .id(java.util.UUID.randomUUID().toString())
                        .timestamp(java.time.Instant.now().toString())
                        .body(body)
                        .build();

                com.langfuse.client.resources.ingestion.requests.IngestionRequest ingestionRequest = com.langfuse.client.resources.ingestion.requests.IngestionRequest
                        .builder()
                        .batch(java.util.List.of(
                                com.langfuse.client.resources.ingestion.types.IngestionEvent.generationCreate(event)))
                        .build();

                langfuseClient.ingestion().batch(ingestionRequest);
            } catch (Exception e) {
                log.warn("Failed to send trace to Langfuse", e);
            }
        }
    }

    /**
     * Extract entities and relationships from text using DeepSeek
     */
    public GraphData extractEntities(String text) {
        try {
            String systemPrompt = """
                    You are an expert Knowledge Graph engineer. Your task is to extract entities and relationships from the provided text.
                    Return ONLY a JSON object with the following structure:
                    {
                      "entities": [
                        {"name": "Entity Name", "type": "Person/Company/Location/Concept", "description": "Short description"}
                      ],
                      "relationships": [
                        {"subject": "Entity Name", "predicate": "RELATIONSHIP_TYPE", "object": "Entity Name", "objectType": "Type"}
                      ]
                    }
                    Do not include any markdown formatting or explanation. Just the raw JSON.
                    """;

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system", systemPrompt));
            messages.add(new ChatMessage("user", text));

            // Use JSON mode if supported, otherwise rely on prompt engineering
            ChatRequest request = new ChatRequest(model, messages, 0.3, 2000);

            String requestBody = objectMapper.writeValueAsString(request);

            ChatResponse response = webClient.post()
                    .uri(apiUrl)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(ChatResponse.class)
                    .block();

            if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                String content = response.choices().get(0).message().content();
                // Clean up markdown code blocks if present
                content = content.replace("```json", "").replace("```", "").trim();
                return objectMapper.readValue(content, GraphData.class);
            }

            return new GraphData(List.of(), List.of());
        } catch (Exception e) {
            log.error("Failed to extract entities from DeepSeek", e);
            // Return empty data on failure to not block the pipeline
            return new GraphData(List.of(), List.of());
        }
    }

    public record GraphData(List<EntityData> entities, List<RelationData> relationships) {
    }

    public record EntityData(String name, String type, String description) {
    }

    public record RelationData(String subject, String predicate, String object, String objectType) {
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
