package com.rag.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG Pipeline Service that combines Qdrant retrieval with DeepSeek generation
 */
@Service
@RequiredArgsConstructor
public class RagPipelineService {

    private static final Logger log = LoggerFactory.getLogger(RagPipelineService.class);

    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;
    private final DeepSeekService deepSeekService;

    /**
     * Process a query through the RAG pipeline:
     * 1. Generate embedding for the query
     * 2. Search for similar documents in Qdrant
     * 3. Use retrieved context with DeepSeek to generate answer
     */
    public String processQuery(String query) {
        log.info("Processing query: {}", query);

        // Step 1: Generate embedding for the query
        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        log.debug("Generated query embedding");

        // Step 2: Search for similar documents in Qdrant
        List<QdrantService.SearchResult> searchResults = qdrantService.searchSimilar(queryEmbedding, 3);
        log.info("Found {} similar documents", searchResults.size());

        // Step 3: Build context from retrieved documents
        String context = buildContext(searchResults);
        log.debug("Built context from retrieved documents");

        // Step 4: Generate response using DeepSeek with context
        String response = deepSeekService.generateResponse(query, context);
        log.info("Generated response from DeepSeek");

        return response;
    }

    /**
     * Index documents into Qdrant
     */
    public void indexDocuments(List<String> texts) {
        log.info("Indexing {} documents", texts.size());

        // Generate embeddings for all texts
        List<float[]> embeddings = embeddingService.generateEmbeddings(texts);

        // Store documents in Qdrant
        for (int i = 0; i < texts.size(); i++) {
            String id = java.util.UUID.randomUUID().toString();
            qdrantService.storeDocument(id, texts.get(i), embeddings.get(i));
        }

        log.info("Successfully indexed {} documents", texts.size());
    }

    /**
     * Build context string from search results
     */
    private String buildContext(List<QdrantService.SearchResult> results) {
        if (results.isEmpty()) {
            return "No relevant context found.";
        }

        return results.stream()
                .map(result -> "[Score: %.2f] %s".formatted(result.getScore(), result.getText()))
                .collect(Collectors.joining("\n\n"));
    }
}
