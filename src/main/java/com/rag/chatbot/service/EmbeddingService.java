package com.rag.chatbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating embeddings
 * Note: For production, integrate with FastEmbed service via HTTP or use ONNX
 * models
 * This implementation uses a simple hash-based approach for demonstration
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private static final int EMBEDDING_SIZE = 384;
    private boolean initialized = true;

    public EmbeddingService() {
        log.info("Embedding service initialized (using hash-based method)");
        log.info("For production, integrate with FastEmbed service or ONNX models");
    }

    /**
     * Generate embeddings for the given text
     * Note: This is a simplified implementation. In production, you would:
     * 1. Call FastEmbed service via HTTP
     * 2. Use ONNX models with proper tokenization
     * 3. Use Java-based embedding libraries
     */
    public float[] generateEmbedding(String text) {
        if (!initialized) {
            throw new IllegalStateException("Embedding service not initialized");
        }

        return generateSimpleEmbedding(text);
    }

    /**
     * Generate a simple embedding vector (fallback method)
     * In production, this should be replaced with actual FastEmbed/ONNX inference
     */
    private float[] generateSimpleEmbedding(String text) {
        float[] embedding = new float[EMBEDDING_SIZE];

        // Simple hash-based embedding (for demonstration)
        // Replace with actual model inference in production
        String normalized = text.toLowerCase().trim();
        Random random = new Random(normalized.hashCode());

        for (int i = 0; i < EMBEDDING_SIZE; i++) {
            embedding[i] = (float) (random.nextGaussian() * 0.1);
        }

        // Normalize the vector
        float norm = 0.0f;
        for (float value : embedding) {
            norm += value * value;
        }
        norm = (float) Math.sqrt(norm);

        if (norm > 0) {
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] /= norm;
            }
        }

        return embedding;
    }

    /**
     * Generate embeddings for multiple texts
     */
    public List<float[]> generateEmbeddings(List<String> texts) {
        return texts.stream()
                .map(this::generateEmbedding)
                .collect(Collectors.toList());
    }

    public int getEmbeddingSize() {
        return EMBEDDING_SIZE;
    }
}
