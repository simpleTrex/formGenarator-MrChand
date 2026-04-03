package com.adaptivebp.modules.workflow.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.adaptivebp.modules.workflow.model.enums.InstanceStatus;

@Document(collection = "workflow_instances")
@CompoundIndexes({
        @CompoundIndex(name = "domain_app_status_idx", def = "{'domainId':1,'appId':1,'status':1}"),
        @CompoundIndex(name = "workflow_definition_idx", def = "{'workflowDefinitionId':1}"),
        @CompoundIndex(name = "started_by_status_idx", def = "{'startedBy':1,'status':1}"),
        @CompoundIndex(name = "current_step_status_idx", def = "{'currentStepId':1,'status':1}")
})
public class WorkflowInstance {

    @Id
    private String id;
    private String workflowDefinitionId;
    private int workflowVersion;
    private String domainId;
    private String appId;
    private InstanceStatus status = InstanceStatus.ACTIVE;
    private String currentStepId;
    private Map<String, Object> stepRecords = new HashMap<>();
    /**
     * The single accumulated record for this workflow instance.
     * Each step appends its editable-field values here.
     * Edge statusLabels set the "status" key.
     */
    private Map<String, Object> primaryRecord = new HashMap<>();
    private List<InstanceHistory> history = new ArrayList<>();
    private String startedBy;
    private Instant startedAt = Instant.now();
    private Instant completedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkflowDefinitionId() {
        return workflowDefinitionId;
    }

    public void setWorkflowDefinitionId(String workflowDefinitionId) {
        this.workflowDefinitionId = workflowDefinitionId;
    }

    public int getWorkflowVersion() {
        return workflowVersion;
    }

    public void setWorkflowVersion(int workflowVersion) {
        this.workflowVersion = workflowVersion;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public InstanceStatus getStatus() {
        return status;
    }

    public void setStatus(InstanceStatus status) {
        this.status = status;
    }

    public String getCurrentStepId() {
        return currentStepId;
    }

    public void setCurrentStepId(String currentStepId) {
        this.currentStepId = currentStepId;
    }

    public Map<String, Object> getStepRecords() {
        return stepRecords;
    }

    public void setStepRecords(Map<String, Object> stepRecords) {
        this.stepRecords = stepRecords != null ? stepRecords : new HashMap<>();
    }

    public Map<String, Object> getPrimaryRecord() {
        if (primaryRecord == null) primaryRecord = new HashMap<>();
        return primaryRecord;
    }

    public void setPrimaryRecord(Map<String, Object> primaryRecord) {
        this.primaryRecord = primaryRecord != null ? primaryRecord : new HashMap<>();
    }

    public List<InstanceHistory> getHistory() {
        return history;
    }

    public void setHistory(List<InstanceHistory> history) {
        this.history = history != null ? history : new ArrayList<>();
    }

    public String getStartedBy() {
        return startedBy;
    }

    public void setStartedBy(String startedBy) {
        this.startedBy = startedBy;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
