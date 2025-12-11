package com.rag.chatbot.service;

import com.rag.chatbot.config.DeepSeekProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.eq; // removed unused
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link DeepSeekService} ensuring that the request is built
 * correctly and the response is parsed.
 */
class DeepSeekServiceTest {

    private DeepSeekService deepSeekService;
    private WebClient mockWebClient;
    private WebClient.RequestBodyUriSpec mockRequestBodyUriSpec;
    private WebClient.RequestBodySpec mockRequestBodySpec;
    private WebClient.RequestHeadersSpec mockRequestHeadersSpec;
    private WebClient.ResponseSpec mockResponseSpec;
    private com.langfuse.client.LangfuseClient mockLangfuseClient;
    private com.langfuse.client.resources.ingestion.IngestionClient mockIngestionClient;

    @BeforeEach
    void setUp() {
        // Create dummy properties
        DeepSeekProperties props = new DeepSeekProperties();

        try {
            Field apiKeyField = DeepSeekProperties.class.getDeclaredField("apiKey");
            apiKeyField.setAccessible(true);
            apiKeyField.set(props, "test-key");
            Field apiUrlField = DeepSeekProperties.class.getDeclaredField("apiUrl");
            apiUrlField.setAccessible(true);
            apiUrlField.set(props, "http://localhost:8080/v1/chat/completions");
            Field modelField = DeepSeekProperties.class.getDeclaredField("model");
            modelField.setAccessible(true);
            modelField.set(props, "deepseek-chat");

            // Mock Langfuse
            mockLangfuseClient = mock(com.langfuse.client.LangfuseClient.class);
            mockIngestionClient = mock(com.langfuse.client.resources.ingestion.IngestionClient.class);
            when(mockLangfuseClient.ingestion()).thenReturn(mockIngestionClient);

            // Re-instantiate service with populated props
            deepSeekService = new DeepSeekService(props, mockLangfuseClient);

            // Mock the WebClient chain with correct types
            mockWebClient = mock(WebClient.class);
            mockRequestBodyUriSpec = mock(RequestBodyUriSpec.class);
            mockRequestBodySpec = mock(RequestBodySpec.class);
            mockRequestHeadersSpec = mock(RequestHeadersSpec.class);
            mockResponseSpec = mock(ResponseSpec.class);

            when(mockWebClient.post()).thenReturn(mockRequestBodyUriSpec);
            when(mockRequestBodyUriSpec.uri(anyString())).thenReturn(mockRequestBodySpec);
            when(mockRequestBodySpec.bodyValue(any())).thenReturn(mockRequestHeadersSpec);
            when(mockRequestHeadersSpec.retrieve()).thenReturn(mockResponseSpec);

            // Inject the mock WebClient into the service via reflection (as it is final and
            // created in constructor)
            Field webClientField = DeepSeekService.class.getDeclaredField("webClient");
            webClientField.setAccessible(true);
            webClientField.set(deepSeekService, mockWebClient);

        } catch (Exception e) {
            fail("Setup failed: " + e.getMessage());
        }
    }

    @Test
    void testGenerateResponse() {
        String userMessage = "What is the capital of France?";
        String context = "Paris is the capital city of France.";

        // Create response objects directly using package-private records
        DeepSeekService.ChatMessage message = new DeepSeekService.ChatMessage("assistant", "mocked response");
        DeepSeekService.ChatResponse.Choice choice = new DeepSeekService.ChatResponse.Choice(message);
        DeepSeekService.ChatResponse chatResponse = new DeepSeekService.ChatResponse(java.util.List.of(choice));

        when(mockResponseSpec.bodyToMono(DeepSeekService.ChatResponse.class)).thenReturn(Mono.just(chatResponse));

        String response = deepSeekService.generateResponse(userMessage, context);
        assertEquals("mocked response", response);

        // Verify that the request was built with correct headers
        verify(mockWebClient).post();
        verify(mockRequestBodyUriSpec).uri("http://localhost:8080/v1/chat/completions");
        verify(mockRequestBodySpec).bodyValue(any());
    }
}
