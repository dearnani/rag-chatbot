package com.rag.chatbot.repository;

import com.rag.chatbot.domain.GraphEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GraphEntityRepository extends Neo4jRepository<GraphEntity, Long> {

    Optional<GraphEntity> findByName(String name);

    @Query("MATCH (n:GraphEntity) WHERE n.name CONTAINS $name RETURN n")
    List<GraphEntity> searchByName(String name);

    // Find entities related to a given entity name (simple 1-hop)
    @Query("MATCH (n:GraphEntity {name: $name})-[r]->(m:GraphEntity) RETURN m")
    List<GraphEntity> findRelatedEntities(String name);
}
