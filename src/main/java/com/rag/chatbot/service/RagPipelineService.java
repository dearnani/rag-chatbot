package com.rag.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private final EmbeddingService embeddingService;
    @Autowired
    private final QdrantService qdrantService;
    @Autowired
    private final DeepSeekService deepSeekService;
    @Autowired
    private final GraphService graphService;

    /**
     * Process a query through the RAG pipeline:
     * 1. Generate embedding for the query
     * 2. Search for similar documents in Qdrant
     * 3. Fetch related context from Knowledge Graph
     * 4. Use retrieved context with DeepSeek to generate answer
     */
    public String processQuery(String query) {
        log.info("Processing query: {}", query);

        // Step 1: Generate embedding for the query
        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        log.debug("Generated query embedding");

        // Step 2: Search for similar documents in Qdrant
        List<QdrantService.SearchResult> searchResults = qdrantService.searchSimilar(queryEmbedding, 3);
        log.info("Found {} similar documents", searchResults.size());

        // Step 3: Fetch graph context
        String graphContext = graphService.getGraphContext(query);
        log.info("Retrieved graph context of length {}", graphContext.length());

        // Step 4: Build context from retrieved documents and graph
        String context = buildContext(searchResults) + "\n\n" + graphContext;
        log.debug("Built combined context");

        // Step 5: Generate response using DeepSeek with context
        String response = deepSeekService.generateResponse(query, context);
        log.info("Generated response from DeepSeek");

        return response;
    }

    /**
     * Index documents into Qdrant and Knowledge Graph
     */
    public void indexDocuments(List<String> texts) {
        log.info("Indexing {} documents", texts.size());

        // Generate embeddings for all texts
        List<float[]> embeddings = embeddingService.generateEmbeddings(texts);

        // Store documents in Qdrant and Graph
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            String id = java.util.UUID.randomUUID().toString();

            // 1. Vector Store
            qdrantService.storeDocument(id, text, embeddings.get(i));

            // 2. Knowledge Graph Extraction
            try {
                DeepSeekService.GraphData graphData = deepSeekService.extractEntities(text);

                // Save entities
                if (graphData.entities() != null) {
                    for (DeepSeekService.EntityData entity : graphData.entities()) {
                        graphService.saveEntity(entity.name(), entity.type(), entity.description());
                    }
                }

                // Save relationships
                if (graphData.relationships() != null) {
                    for (DeepSeekService.RelationData rel : graphData.relationships()) {
                        graphService.createRelationship(rel.subject(), rel.predicate(), rel.object(), rel.objectType());
                    }
                }
            } catch (Exception e) {
                log.error("Error processing graph data for document: {}", id, e);
                // Continue indexing other docs even if graph fails
            }
        }

        log.info("Successfully indexed {} documents", texts.size());
    }

    /**
     * Build context string from search results
     */
    private String buildContext(List<QdrantService.SearchResult> results) {
        if (results.isEmpty()) {
            return "No relevant vector context found.";
        }

        return results.stream()
                .map(result -> "[Score: %.2f] %s".formatted(result.getScore(), result.getText()))
                .collect(Collectors.joining("\n\n"));
    }
}
