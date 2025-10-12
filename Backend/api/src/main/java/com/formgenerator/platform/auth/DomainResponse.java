package com.formgenerator.platform.auth;

import java.util.Date;
import java.util.Map;

/**
 * Response DTO for domain information.
 */
public class DomainResponse {
    private String id;
    private String name;
    private String ownerUserId;
    private Date createdAt;
    private Map<String, Object> metadata;

    public DomainResponse() {}

    public DomainResponse(Domain domain) {
        this.id = domain.getId();
        this.name = domain.getName();
        this.ownerUserId = domain.getOwnerUserId();
        this.createdAt = domain.getCreatedAt();
        this.metadata = domain.getMetadata();
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