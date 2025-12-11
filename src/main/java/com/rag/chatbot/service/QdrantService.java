package com.rag.chatbot.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for interacting with Qdrant vector database via REST API
 */
@Service
public class QdrantService {

    private static final Logger log = LoggerFactory.getLogger(QdrantService.class);

    private final WebClient webClient;
    private final String collectionName;
    private final int vectorSize;

    public QdrantService(WebClient qdrantWebClient,
            com.rag.chatbot.config.QdrantProperties qdrantProperties) {
        this.webClient = qdrantWebClient;
        this.collectionName = qdrantProperties.getCollectionName();
        this.vectorSize = qdrantProperties.getVectorSize();
        new ObjectMapper();
    }

    /**
     * Initialize the Qdrant collection if it doesn't exist
     */
    public void initializeCollection() {
        try {
            // Check if collection exists
            CollectionInfo info = webClient.get()
                    .uri("/collections/{collection}", collectionName)
                    .retrieve()
                    .bodyToMono(CollectionInfo.class)
                    .onErrorReturn(new CollectionInfo(null))
                    .block();

            if (info == null || info.status() == null) {
                // Create collection
                OptimizerConfig optimizerConfig = new OptimizerConfig(10); // Index even with few points
                CreateCollectionRequest request = new CreateCollectionRequest(
                        new VectorConfig(vectorSize, "Cosine"),
                        optimizerConfig);

                webClient.put()
                        .uri("/collections/{collection}", collectionName)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                log.info("Created Qdrant collection: {}", collectionName);
            } else {
                log.info("Qdrant collection already exists: {}", collectionName);
            }

            // Always update optimizer config to ensure indexing threshold is low enough for
            // small datasets
            updateOptimizerConfig();

        } catch (Exception e) {
            log.error("Failed to initialize Qdrant collection", e);
            throw new RuntimeException("Failed to initialize Qdrant collection", e);
        }
    }

    /**
     * Store a document with its embedding in Qdrant
     */
    public void storeDocument(String id, String text, float[] embedding) {
        try {
            List<Double> vector = new ArrayList<>();
            for (float f : embedding) {
                vector.add((double) f);
            }

            Point point = new Point(id, vector, Map.of("text", text));

            UpsertPointsRequest request = new UpsertPointsRequest(
                    Collections.singletonList(point));

            webClient.put()
                    .uri("/collections/{collection}/points", collectionName)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.debug("Stored document {} in Qdrant", id);
        } catch (Exception e) {
            log.error("Failed to store document in Qdrant", e);
            throw new RuntimeException("Failed to store document", e);
        }
    }

    /**
     * Store multiple documents
     */
    public void storeDocuments(Map<String, DocumentWithEmbedding> documents) {
        try {
            List<Point> points = documents.entrySet().stream()
                    .map(entry -> {
                        String id = entry.getKey();
                        DocumentWithEmbedding doc = entry.getValue();

                        List<Double> vector = new ArrayList<>();
                        for (float f : doc.getEmbedding()) {
                            vector.add((double) f);
                        }

                        return new Point(id, vector, Map.of("text", doc.getText()));
                    })
                    .collect(Collectors.toList());

            UpsertPointsRequest request = new UpsertPointsRequest(points);

            webClient.put()
                    .uri("/collections/{collection}/points", collectionName)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Stored {} documents in Qdrant", documents.size());
        } catch (Exception e) {
            log.error("Failed to store documents in Qdrant", e);
            throw new RuntimeException("Failed to store documents", e);
        }
    }

    /**
     * Search for similar documents using a query embedding
     */
    public List<SearchResult> searchSimilar(float[] queryEmbedding, int limit) {
        try {
            List<Double> queryVector = new ArrayList<>();
            for (float f : queryEmbedding) {
                queryVector.add((double) f);
            }

            SearchRequest request = new SearchRequest(queryVector, limit, true);

            SearchResponse response = webClient.post()
                    .uri("/collections/{collection}/points/search", collectionName)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(SearchResponse.class)
                    .block();

            if (response == null || response.result() == null) {
                return Collections.emptyList();
            }

            return response.result().stream()
                    .map(scoredPoint -> {
                        String text = "";
                        if (scoredPoint.payload() != null && scoredPoint.payload().containsKey("text")) {
                            Object textObj = scoredPoint.payload().get("text");
                            text = textObj != null ? textObj.toString() : "";
                        }
                        return new SearchResult(
                                scoredPoint.id(),
                                text,
                                scoredPoint.score() != null ? scoredPoint.score().floatValue() : 0.0f);
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to search in Qdrant", e);
            throw new RuntimeException("Failed to search documents", e);
        }
    }

    /**
     * Update optimizer configuration for existing collection
     */
    private void updateOptimizerConfig() {
        try {
            OptimizerConfig optimizerConfig = new OptimizerConfig(10);
            UpdateCollectionRequest request = new UpdateCollectionRequest(optimizerConfig);

            webClient.patch()
                    .uri("/collections/{collection}", collectionName)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.debug("Updated optimizer config for collection: {}", collectionName);
        } catch (Exception e) {
            log.warn("Failed to update optimizer config for collection (non-fatal): {}", e.getMessage());
        }
    }

    // DTOs for Qdrant API
    private record CreateCollectionRequest(VectorConfig vectors,
            @JsonProperty("optimizers_config") OptimizerConfig optimizersConfig) {
    }

    private record VectorConfig(int size, String distance) {
    }

    private record CollectionInfo(String status) {
    }

    private record UpsertPointsRequest(List<Point> points) {
    }

    private record Point(String id, List<Double> vector, Map<String, Object> payload) {
    }

    private record SearchRequest(
            List<Double> vector,
            Integer limit,
            @JsonProperty("with_payload") Boolean withPayload) {
    }

    private record SearchResponse(List<ScoredPoint> result) {
    }

    private record ScoredPoint(String id, Double score, Map<String, Object> payload) {
    }

    private record OptimizerConfig(@JsonProperty("indexing_threshold") Integer indexingThreshold) {
    }

    private record UpdateCollectionRequest(@JsonProperty("optimizers_config") OptimizerConfig optimizersConfig) {
    }

    /**
     * Inner class to hold document with embedding
     */
    public static class DocumentWithEmbedding {
        private final String text;
        private final float[] embedding;

        public DocumentWithEmbedding(String text, float[] embedding) {
            this.text = text;
            this.embedding = embedding;
        }

        public String getText() {
            return text;
        }

        public float[] getEmbedding() {
            return embedding;
        }
    }

    /**
     * Inner class to hold search results
     */
    public static class SearchResult {
        private final String id;
        private final String text;
        private final float score;

        public SearchResult(String id, String text, float score) {
            this.id = id;
            this.text = text;
            this.score = score;
        }

        public String getId() {
            return id;
        }

        public String getText() {
            return text;
        }

        public float getScore() {
            return score;
        }
    }
}
