package com.rag.chatbot.controller;

import com.rag.chatbot.service.RagPipelineService;
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
public class RagController {
    
    private static final Logger log = LoggerFactory.getLogger(RagController.class);

    private final RagPipelineService ragPipelineService;

    /**
     * Process a query through the RAG pipeline
     */
    @PostMapping("/query")
    public ResponseEntity<QueryResponse> query(@RequestBody QueryRequest request) {
        try {
            String response = ragPipelineService.processQuery(request.getQuery());
            return ResponseEntity.ok(new QueryResponse(response));
        } catch (Exception e) {
            log.error("Error processing query", e);
            return ResponseEntity.internalServerError()
                    .body(new QueryResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Index documents into the vector database
     */
    @PostMapping("/index")
    public ResponseEntity<IndexResponse> index(@RequestBody IndexRequest request) {
        try {
            ragPipelineService.indexDocuments(request.getTexts());
            return ResponseEntity.ok(new IndexResponse("Successfully indexed " + request.getTexts().size() + " documents"));
        } catch (Exception e) {
            log.error("Error indexing documents", e);
            return ResponseEntity.internalServerError()
                    .body(new IndexResponse("Error: " + e.getMessage()));
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

