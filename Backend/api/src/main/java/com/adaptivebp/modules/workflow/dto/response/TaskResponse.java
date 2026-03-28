package com.adaptivebp.modules.workflow.dto.response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskResponse {
    private String instanceId;
    private String workflowName;
    private String currentStepName;
    private Map<String, Object> startedBy = new HashMap<>();
    private Instant startedAt;
    private Instant waitingSince;
    private List<Map<String, Object>> availableEdges = new ArrayList<>();
    private Map<String, Object> summary = new HashMap<>();

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public String getCurrentStepName() {
        return currentStepName;
    }

    public void setCurrentStepName(String currentStepName) {
        this.currentStepName = currentStepName;
    }

    public Map<String, Object> getStartedBy() {
        return startedBy;
    }

    public void setStartedBy(Map<String, Object> startedBy) {
        this.startedBy = startedBy != null ? startedBy : new HashMap<>();
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getWaitingSince() {
        return waitingSince;
    }

    public void setWaitingSince(Instant waitingSince) {
        this.waitingSince = waitingSince;
    }

    public List<Map<String, Object>> getAvailableEdges() {
        return availableEdges;
    }

    public void setAvailableEdges(List<Map<String, Object>> availableEdges) {
        this.availableEdges = availableEdges != null ? availableEdges : new ArrayList<>();
    }

    public Map<String, Object> getSummary() {
        return summary;
    }

    public void setSummary(Map<String, Object> summary) {
        this.summary = summary != null ? summary : new HashMap<>();
    }
}
