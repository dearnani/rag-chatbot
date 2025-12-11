package com.rag.chatbot.controller;

import com.rag.chatbot.service.RagPipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for RAG pipeline endpoints
 */
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@Tag(name = "RAG Bot API", description = "Endpoints for RAG Chatbot operations")
public class RagController {

    private static final Logger log = LoggerFactory.getLogger(RagController.class);

    private final RagPipelineService ragPipelineService;

    /**
     * Process a query through the RAG pipeline
     */
    @Operation(summary = "Query the RAG pipeline", description = "Sends a natural language query to the RAG pipeline and returns the generated response.")
    @PostMapping("/query")
    public ResponseEntity<QueryResponse> query(@RequestBody QueryRequest request) {
        try {
            String response = ragPipelineService.processQuery(request.getQuery());
            return ResponseEntity.ok(new QueryResponse(response));
        } catch (Exception e) {
            log.error("Error processing query", e);
            return ResponseEntity.internalServerError()
                    .body(new QueryResponse("Error processing query: " + e.toString()));
        }
    }

    /**
     * Index documents into the vector database
     */
    @Operation(summary = "Index Documents", description = "Ingests a list of text documents into the vector database for retrieval.")
    @PostMapping("/index")
    public ResponseEntity<IndexResponse> index(@RequestBody IndexRequest request) {
        try {
            ragPipelineService.indexDocuments(request.getTexts());
            return ResponseEntity
                    .ok(new IndexResponse("Successfully indexed " + request.getTexts().size() + " documents"));
        } catch (Exception e) {
            log.error("Error indexing documents", e);
            return ResponseEntity.internalServerError()
                    .body(new IndexResponse("Error indexing documents: " + e.toString()));
        }
    }

    @Data
    private static class QueryRequest {
        private String query;

        public String getQuery() {
            return query;
        }
    }

    @Data
    private static class QueryResponse {
        private String response;

        public QueryResponse(String response) {
            this.response = response;
        }
    }

    @Data
    private static class IndexRequest {
        private List<String> texts;

        public List<String> getTexts() {
            return texts;
        }
    }

    @Data
    private static class IndexResponse {
        private String message;

        public IndexResponse(String message) {
            this.message = message;
        }
    }
}
