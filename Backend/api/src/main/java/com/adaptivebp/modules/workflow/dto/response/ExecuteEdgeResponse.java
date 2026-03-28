package com.adaptivebp.modules.workflow.dto.response;

public class ExecuteEdgeResponse {
    private String instanceId;
    private String status;
    private String currentStepId;
    private String previousEdge;
    private String nextStepName;

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
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

    public String getPreviousEdge() {
        return previousEdge;
    }

    public void setPreviousEdge(String previousEdge) {
        this.previousEdge = previousEdge;
    }

    public String getNextStepName() {
        return nextStepName;
    }

    public void setNextStepName(String nextStepName) {
        this.nextStepName = nextStepName;
    }
}
