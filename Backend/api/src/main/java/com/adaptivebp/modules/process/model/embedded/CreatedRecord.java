package com.adaptivebp.modules.process.model.embedded;

import java.time.Instant;

public class CreatedRecord {

    private String modelId;
    private String recordId;
    private Instant createdAt = Instant.now();

    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
