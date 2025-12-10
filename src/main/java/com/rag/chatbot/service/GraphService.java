package com.rag.chatbot.service;

import com.rag.chatbot.domain.GraphEntity;
import com.rag.chatbot.domain.GraphRelation;
import com.rag.chatbot.repository.GraphEntityRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GraphService {

    private static final Logger log = LoggerFactory.getLogger(GraphService.class);
    private final GraphEntityRepository graphEntityRepository;

    @Transactional
    public GraphEntity saveEntity(String name, String type, String description) {
        Optional<GraphEntity> existing = graphEntityRepository.findByName(name);
        if (existing.isPresent()) {
            return existing.get();
        }
        GraphEntity entity = new GraphEntity(name, type);
        entity.setDescription(description);
        return graphEntityRepository.save(entity);
    }

    @Transactional
    public void createRelationship(String subjectName, String predicate, String objectName, String objectType) {
        GraphEntity subject = saveEntity(subjectName, "Unknown", null); // We might need better type inference
        GraphEntity object = saveEntity(objectName, objectType, null);

        // Check if relationship already exists to avoid duplicates is a bit complex in
        // pure OGM without custom queries,
        // so for MVP we'll just add it. In prod, we'd check existence.
        GraphRelation relation = new GraphRelation(predicate, object);
        subject.addRelationship(relation);
        graphEntityRepository.save(subject);
        log.info("Created relationship: ({}) -[{}]-> ({})", subjectName, predicate, objectName);
    }

    public List<GraphEntity> findRelatedEntities(String name) {
        return graphEntityRepository.findRelatedEntities(name);
    }

    public String getGraphContext(String query) {
        // Simple logic: extract potential keywords from query (naive split) or usage of
        // DeepSeek to get keywords.
        // For now, let's just search by exact name match if the query contains the
        // entity name.
        // This is a placeholder for more advanced graph traversal options.

        List<GraphEntity> entities = graphEntityRepository.searchByName(query); // Simplistic
        if (entities.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder("Graph Context:\n");
        for (GraphEntity entity : entities) {
            context.append(String.format("Entity: %s (%s)\n", entity.getName(), entity.getType()));
            if (entity.getDescription() != null) {
                context.append(String.format("Description: %s\n", entity.getDescription()));
            }
            // Fetch relations (lazy loading might require transactional scope or explicit
            // fetch if not eager)
            // For MVP assuming relations are fetched or we query them
        }
        return context.toString();
    }
}
