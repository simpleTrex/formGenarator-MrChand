package com.adaptivebp.modules.uibuilder.model;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "component_definitions")
@CompoundIndexes({
    @CompoundIndex(name = "app_comp_idx", def = "{'appId':1,'domainSlug':1}")
})
public class ComponentDefinition {
    @Id
    private String id;
    private String appId;
    private String appSlug;
    private String domainSlug;
    private String name;
    private PrimitiveType primitiveType;
    private String modelId;
    private Map<String, Object> config;
    @CreatedDate
    private Instant createdAt = Instant.now();
    @LastModifiedDate
    private Instant updatedAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getAppSlug() { return appSlug; }
    public void setAppSlug(String appSlug) { this.appSlug = appSlug; }
    public String getDomainSlug() { return domainSlug; }
    public void setDomainSlug(String domainSlug) { this.domainSlug = domainSlug; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PrimitiveType getPrimitiveType() { return primitiveType; }
    public void setPrimitiveType(PrimitiveType primitiveType) { this.primitiveType = primitiveType; }
    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }
    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
