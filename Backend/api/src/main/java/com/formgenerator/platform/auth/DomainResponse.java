package com.formgenerator.platform.auth;

import java.util.Date;
import java.util.Map;

/**
 * Response DTO for domain information.
 */
public class DomainResponse {
    private String id;
    private String name;
    private String slug;
    private String ownerUserId;
    private Date createdAt;
    private Map<String, Object> metadata;
    private String description;
    private String industry;

    public DomainResponse() {}

    public DomainResponse(Domain domain) {
        this.id = domain.getId();
        this.name = domain.getName();
        this.slug = domain.getSlug();
        this.ownerUserId = domain.getOwnerUserId();
        this.createdAt = domain.getCreatedAt();
        this.metadata = domain.getMetadata();
        this.description = domain.getDescription();
        this.industry = domain.getIndustry();
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

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
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
}