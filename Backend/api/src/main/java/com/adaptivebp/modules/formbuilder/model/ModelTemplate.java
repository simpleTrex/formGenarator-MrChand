package com.adaptivebp.modules.formbuilder.model;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Model templates provide pre-built data model definitions for common business entities.
 * Templates can be applied when creating new models to save time and ensure best practices.
 */
@Document(collection = "model_templates")
public class ModelTemplate {

    @Id
    private String id;
    private String name;
    private String description;
    private String category; // HR, Finance, Operations, etc.
    private List<DomainModelField> fields;
    @CreatedDate
    private Instant createdAt = Instant.now();

    // Constructors
    public ModelTemplate() {}

    public ModelTemplate(String id, String name, String description, String category, List<DomainModelField> fields) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.fields = fields;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public List<DomainModelField> getFields() { return fields; }
    public void setFields(List<DomainModelField> fields) { this.fields = fields; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}