package com.adaptivebp.modules.workflow.dto.response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.adaptivebp.modules.workflow.model.WorkflowEdge;
import com.adaptivebp.modules.workflow.model.WorkflowInstance;

public class WorkflowInstanceResponse {
    private String instanceId;
    private String workflowDefinitionId;
    private int workflowVersion;
    private String status;
    private String currentStepId;
    private String currentStepName;
    private String startedBy;
    private Instant startedAt;
    private Instant completedAt;
    private List<WorkflowEdge> availableEdges = new ArrayList<>();
    private Map<String, Object> summary = new HashMap<>();

    public static WorkflowInstanceResponse from(WorkflowInstance instance) {
        WorkflowInstanceResponse response = new WorkflowInstanceResponse();
        response.setInstanceId(instance.getId());
        response.setWorkflowDefinitionId(instance.getWorkflowDefinitionId());
        response.setWorkflowVersion(instance.getWorkflowVersion());
        response.setStatus(instance.getStatus() != null ? instance.getStatus().name() : null);
        response.setCurrentStepId(instance.getCurrentStepId());
        response.setStartedBy(instance.getStartedBy());
        response.setStartedAt(instance.getStartedAt());
        response.setCompletedAt(instance.getCompletedAt());
        return response;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentStepId() {
        return currentStepId;
    }

    public void setCurrentStepId(String currentStepId) {
        this.currentStepId = currentStepId;
    }

    public String getCurrentStepName() {
        return currentStepName;
    }

    public void setCurrentStepName(String currentStepName) {
        this.currentStepName = currentStepName;
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

    public List<WorkflowEdge> getAvailableEdges() {
        return availableEdges;
    }

    public void setAvailableEdges(List<WorkflowEdge> availableEdges) {
        this.availableEdges = availableEdges != null ? availableEdges : new ArrayList<>();
    }

    public Map<String, Object> getSummary() {
        return summary;
    }

    public void setSummary(Map<String, Object> summary) {
        this.summary = summary != null ? summary : new HashMap<>();
    }
}
