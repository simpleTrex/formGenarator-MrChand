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

    // URL-safe unique identifier for domain routing (e.g., "abc-corp")
    @Indexed(unique = true)
    private String slug;
    
    @NotBlank
    @Indexed
    private String ownerUserId;
    
    private Date createdAt;
    
    // Optional details captured during business registration
    @Size(max = 500)
    private String description;

    @Size(max = 100)
    private String industry;

    private Map<String, Object> metadata = new HashMap<>();

    public Domain() {
        this.createdAt = new Date();
    }

    public Domain(String name, String slug, String ownerUserId) {
        this.name = name;
        this.slug = slug;
        this.ownerUserId = ownerUserId;
        this.createdAt = new Date();
    }

    // Backward-compatible constructor used by existing flows
    public Domain(String name, String ownerUserId) {
        this.name = name;
        this.slug = toSlug(name);
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

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    private static String toSlug(String input) {
        if (input == null) return null;
        String s = input.trim().toLowerCase();
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+", "").replaceAll("-+$", "");
        return s;
    }
}