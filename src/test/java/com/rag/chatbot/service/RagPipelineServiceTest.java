package com.rag.chatbot.service;

import com.rag.chatbot.service.DeepSeekService.EntityData;
import com.rag.chatbot.service.DeepSeekService.GraphData;
import com.rag.chatbot.service.DeepSeekService.RelationData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagPipelineServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private QdrantService qdrantService;

    @Mock
    private DeepSeekService deepSeekService;

    @Mock
    private GraphService graphService;

    @InjectMocks
    private RagPipelineService ragPipelineService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testProcessQuery_callsGraphService() {
        String query = "Who is Alice?";
        float[] embedding = new float[] { 0.1f, 0.2f };

        when(embeddingService.generateEmbedding(query)).thenReturn(embedding);
        when(qdrantService.searchSimilar(any(), eq(3))).thenReturn(Collections.emptyList());
        when(graphService.getGraphContext(query)).thenReturn("Graph Context: Alice is a person.");
        when(deepSeekService.generateResponse(anyString(), anyString())).thenReturn("Alice is a person.");

        String response = ragPipelineService.processQuery(query);

        assertEquals("Alice is a person.", response);
        verify(graphService).getGraphContext(query);
        verify(deepSeekService).generateResponse(eq(query), anyString());
    }

    @Test
    void testIndexDocuments_extractsEntities() {
        String text = "Alice works at Google.";
        List<String> texts = List.of(text);
        float[] embedding = new float[] { 0.1f, 0.1f };

        when(embeddingService.generateEmbeddings(texts)).thenReturn(List.of(embedding));

        GraphData graphData = new GraphData(
                List.of(new EntityData("Alice", "Person", null), new EntityData("Google", "Company", null)),
                List.of(new RelationData("Alice", "WORKS_AT", "Google", "Company")));
        when(deepSeekService.extractEntities(text)).thenReturn(graphData);

        ragPipelineService.indexDocuments(texts);

        verify(qdrantService).storeDocument(anyString(), eq(text), eq(embedding));
        verify(deepSeekService).extractEntities(text);
        verify(graphService).saveEntity("Alice", "Person", null);
        verify(graphService).saveEntity("Google", "Company", null);
        verify(graphService).createRelationship("Alice", "WORKS_AT", "Google", "Company");
    }

    @Test
    void testProcessQuery_noSearchResults() {
        String query = "Unknown topic";
        float[] embedding = new float[] { 0.3f, 0.4f };

        when(embeddingService.generateEmbedding(query)).thenReturn(embedding);
        when(qdrantService.searchSimilar(any(), eq(3))).thenReturn(Collections.emptyList());
        when(graphService.getGraphContext(query)).thenReturn("");
        // Note: buildContext returns "No relevant vector context found." when list is
        // empty
        when(deepSeekService.generateResponse(eq(query), anyString())).thenReturn("I don't know.");

        String response = ragPipelineService.processQuery(query);

        assertEquals("I don't know.", response);
    }

    @Test
    void testIndexDocuments_graphExtractionFailure() {
        String text = "Bad text";
        List<String> texts = List.of(text);
        float[] embedding = new float[] { 0.1f };

        when(embeddingService.generateEmbeddings(texts)).thenReturn(List.of(embedding));
        when(deepSeekService.extractEntities(text)).thenThrow(new RuntimeException("API Error"));

        // Should not throw exception
        ragPipelineService.indexDocuments(texts);

        verify(qdrantService).storeDocument(anyString(), eq(text), eq(embedding));
        verify(deepSeekService).extractEntities(text);
        // Graph service save should NOT be called if extraction fails
        verify(graphService, times(0)).saveEntity(anyString(), anyString(), anyString());
    }
}
