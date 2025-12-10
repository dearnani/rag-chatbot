package com.rag.chatbot.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link QdrantProperties} ensuring default values are set correctly.
 */
class QdrantPropertiesTest {

    @Test
    void testDefaultValues() {
        QdrantProperties props = new QdrantProperties();
        assertEquals("localhost", props.getHost(), "Default host should be localhost");
        assertEquals(6333, props.getPort(), "Default port should be 6333");
        assertEquals("rag-documents", props.getCollectionName(), "Default collection name should be rag-documents");
        assertEquals(384, props.getVectorSize(), "Default vector size should be 384");
    }
}
