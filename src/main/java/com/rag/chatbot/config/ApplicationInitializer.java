package com.rag.chatbot.config;

import com.rag.chatbot.service.QdrantService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initialize Qdrant collection on application startup
 */
@Component
@RequiredArgsConstructor
public class ApplicationInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ApplicationInitializer.class);

    @Autowired
    private QdrantService qdrantService;

    @Override
    public void run(String... args) {
        log.info("Initializing Qdrant collection...");
        qdrantService.initializeCollection();
        log.info("Application initialization complete");
    }
}
