package com.rag.chatbot.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

@Node
@Data
@NoArgsConstructor
public class GraphEntity {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String type; // e.g., "Person", "Company", "Location"

    // Additional properties can be stored as a map or specific fields if needed
    private String description;

    @Relationship(type = "RELATED_TO", direction = Relationship.Direction.OUTGOING)
    private List<GraphRelation> relationships = new ArrayList<>();

    public GraphEntity(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public void addRelationship(GraphRelation relation) {
        this.relationships.add(relation);
    }
}
