package com.adaptivebp.modules.formbuilder.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One row of data for a DomainModel — the actual persisted records
 * that DATA_ACTION nodes write and DATA_VIEW nodes query.
 */
@Document(collection = "model_records")
@CompoundIndexes({
        @CompoundIndex(name = "model_domain_idx", def = "{'modelId':1,'domainId':1}")
})
public class ModelRecord {

    @Id
    private String id;

    /** References DomainModel.id */
    @Indexed
    private String modelId;

    private String domainId;

    /** App that created this record (for scoping) */
    private String appId;

    /** Process instance that triggered the creation */
    private String instanceId;

    /** Actual field data: key = DomainModelField.key, value = user-submitted value */
    private Map<String, Object> data = new HashMap<>();

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    private String createdBy;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }

    public String getDomainId() { return domainId; }
    public void setDomainId(String domainId) { this.domainId = domainId; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
