package com.adaptivebp.modules.process.model;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Process templates provide pre-built workflow definitions for common business processes.
 * Templates can be applied when creating new processes to save time and ensure best practices.
 */
@Document(collection = "process_templates")
public class ProcessTemplate {

    @Id
    private String id;
    private String name;
    private String description;
    private String category; // HR, Finance, Operations, etc.
    private List<ProcessNode> nodes;
    private List<ProcessEdge> edges;
    private List<String> requiredModels; // List of model slugs this template requires
    @CreatedDate
    private Instant createdAt = Instant.now();

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public List<ProcessNode> getNodes() { return nodes; }
    public void setNodes(List<ProcessNode> nodes) { this.nodes = nodes; }
    public List<ProcessEdge> getEdges() { return edges; }
    public void setEdges(List<ProcessEdge> edges) { this.edges = edges; }
    public List<String> getRequiredModels() { return requiredModels; }
    public void setRequiredModels(List<String> requiredModels) { this.requiredModels = requiredModels; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}