package com.formgenerator.platform.auth;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Domain entity represents a business domain/tenant in the AdaptiveBP platform.
 * Each domain has an owner (Business Owner) and can contain multiple users with domain-scoped roles.
 */
@Document(collection = "domains")
public class Domain {
    @Id
    private String id;
    
    @NotBlank
    @Size(min = 3, max = 50)
    private String name;
    
    @NotBlank
    @Indexed
    private String ownerUserId;
    
    private Date createdAt;
    
    private Map<String, Object> metadata = new HashMap<>();

    public Domain() {
        this.createdAt = new Date();
    }

    public Domain(String name, String ownerUserId) {
        this.name = name;
        this.ownerUserId = ownerUserId;
        this.createdAt = new Date();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}