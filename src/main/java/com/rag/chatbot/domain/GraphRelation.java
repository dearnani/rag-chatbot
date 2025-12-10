package com.rag.chatbot.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
@Data
@NoArgsConstructor
public class GraphRelation {

    @Id
    @GeneratedValue
    private Long id;

    private String type; // e.g., "FOUNDED", "ACQUIRED"

    @TargetNode
    private GraphEntity targetEntity;

    public GraphRelation(String type, GraphEntity targetEntity) {
        this.type = type;
        this.targetEntity = targetEntity;
    }
}
