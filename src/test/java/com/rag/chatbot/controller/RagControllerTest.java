package com.rag.chatbot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.chatbot.service.RagPipelineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RagController.class)
class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RagPipelineService ragPipelineService;

    // ApplicationInitializer runs on startup and needs this, so we mock it to
    // prevent context failure
    @MockitoBean
    private com.rag.chatbot.service.QdrantService qdrantService;

    @Test
    void testQuery_success() throws Exception {
        String query = "Hello";
        String response = "Hi there";

        when(ragPipelineService.processQuery(query)).thenReturn(response);

        // Construct request JSON manually to match static inner class structure if
        // needed,
        // or use a map/object if Jackson can handle it.
        // The Controller expects { "query": "..." }
        String requestBody = "{\"query\": \"" + query + "\"}";

        mockMvc.perform(post("/api/rag/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value(response));

        verify(ragPipelineService).processQuery(query);
    }

    @Test
    void testQuery_error() throws Exception {
        String query = "Error";
        when(ragPipelineService.processQuery(query)).thenThrow(new RuntimeException("Processing failed"));

        String requestBody = "{\"query\": \"" + query + "\"}";

        mockMvc.perform(post("/api/rag/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.response").value("Error: Processing failed"));
    }

    @Test
    void testIndex_success() throws Exception {
        List<String> texts = List.of("Doc 1");

        // Mock void method: doNothing key implies default behavior for mocks, so we
        // just verifying it's called.

        String requestBody = "{\"texts\": [\"Doc 1\"]}";

        mockMvc.perform(post("/api/rag/index")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully indexed 1 documents"));

        verify(ragPipelineService).indexDocuments(texts);
    }
}
