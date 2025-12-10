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

    @BeforeEach
    void setUp() {
        // Create dummy properties
        DeepSeekProperties props = new DeepSeekProperties();
        // Since DeepSeekProperties fields are private and have no setters, we still
        // need
        // reflection for the properties object or a better constructor in
        // DeepSeekProperties.
        // Assuming we can't change DeepSeekProperties easily right now, we keep
        // reflection
        // just for the properties object or use a constructor if available.
        // Checking DeepSeekProperties code earlier... it has a default constructor and
        // getters/setters (Lombok data/value usually) or manually defined.
        // The previous test code used reflection for DeepSeekProperties. java beans
        // convention implies setters usually.
        // Let's check if we can modify DeepSeekProperties to have a constructor or
        // setters if needed, but for now
        // we will stick to reflection for the properties if it's a simple POJO without
        // setters.
        // HOWEVER, the previous test code showed it was using declared fields.

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

            // Re-instantiate service with populated props
            // But wait, the service constructor creates the WebClient internally.
            // We need to inject our mock WebClient.
            deepSeekService = new DeepSeekService(props);

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
