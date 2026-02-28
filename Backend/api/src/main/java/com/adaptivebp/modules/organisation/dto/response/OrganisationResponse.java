package com.adaptivebp.modules.organisation.dto.response;

import java.util.Date;
import java.util.Map;

import com.adaptivebp.modules.organisation.model.Organisation;

public class OrganisationResponse {
    private String id;
    private String name;
    private String slug;
    private String ownerUserId;
    private Date createdAt;
    private Map<String, Object> metadata;
    private String description;
    private String industry;

    public OrganisationResponse() {}

    public OrganisationResponse(Organisation org) {
        this.id = org.getId();
        this.name = org.getName();
        this.slug = org.getSlug();
        this.ownerUserId = org.getOwnerUserId();
        this.createdAt = org.getCreatedAt();
        this.metadata = org.getMetadata();
        this.description = org.getDescription();
        this.industry = org.getIndustry();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
}
