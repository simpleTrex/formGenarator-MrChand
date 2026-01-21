package com.formgenerator.api.models.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "domain_models")
@CompoundIndexes({
        @CompoundIndex(name = "domain_slug_idx", def = "{'domainId':1,'slug':1}", unique = true)
})
public class DomainModel {

    @Id
    private String id;

    private String domainId;

    /** slug unique within domain */
    private String slug;

    private String name;

    private String description;

    /** Version number for schema changes. */
    private int version = 1;

    private List<DomainModelField> fields = new ArrayList<>();

    /**
     * Sharing control:
     * - if sharedWithAllApps=true, any app in the domain can access.
     * - else only apps in allowedAppIds can access.
     */
    private boolean sharedWithAllApps = false;

    private Set<String> allowedAppIds = new HashSet<>();

    @CreatedDate
    private Instant createdAt = Instant.now();

    @LastModifiedDate
    private Instant updatedAt = Instant.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<DomainModelField> getFields() {
        return fields;
    }

    public void setFields(List<DomainModelField> fields) {
        this.fields = fields;
    }

    public boolean isSharedWithAllApps() {
        return sharedWithAllApps;
    }

    public void setSharedWithAllApps(boolean sharedWithAllApps) {
        this.sharedWithAllApps = sharedWithAllApps;
    }

    public Set<String> getAllowedAppIds() {
        return allowedAppIds;
    }

    public void setAllowedAppIds(Set<String> allowedAppIds) {
        this.allowedAppIds = allowedAppIds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isAccessibleByAppId(String appId) {
        if (sharedWithAllApps) {
            return true;
        }
        return appId != null && allowedAppIds != null && allowedAppIds.contains(appId);
    }
}
