package com.adaptivebp.modules.workflow.model;

import java.util.ArrayList;
import java.util.List;

public class WorkflowEdge {
    private String id;
    private String name;
    private String targetStepId;
    private boolean isTerminal;
    private List<String> allowedRoles = new ArrayList<>();
    private List<String> allowedUserIds = new ArrayList<>();
    private boolean onlySubmitter;
    private List<String> requiredFields = new ArrayList<>();
    private List<EdgeCondition> conditions = new ArrayList<>();
    private List<AutoAction> autoActions = new ArrayList<>();

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

    public String getTargetStepId() {
        return targetStepId;
    }

    public void setTargetStepId(String targetStepId) {
        this.targetStepId = targetStepId;
    }

    public boolean isTerminal() {
        return isTerminal;
    }

    public void setTerminal(boolean terminal) {
        isTerminal = terminal;
    }

    public List<String> getAllowedRoles() {
        return allowedRoles;
    }

    public void setAllowedRoles(List<String> allowedRoles) {
        this.allowedRoles = allowedRoles != null ? allowedRoles : new ArrayList<>();
    }

    public List<String> getAllowedUserIds() {
        return allowedUserIds;
    }

    public void setAllowedUserIds(List<String> allowedUserIds) {
        this.allowedUserIds = allowedUserIds != null ? allowedUserIds : new ArrayList<>();
    }

    public boolean isOnlySubmitter() {
        return onlySubmitter;
    }

    public void setOnlySubmitter(boolean onlySubmitter) {
        this.onlySubmitter = onlySubmitter;
    }

    public List<String> getRequiredFields() {
        return requiredFields;
    }

    public void setRequiredFields(List<String> requiredFields) {
        this.requiredFields = requiredFields != null ? requiredFields : new ArrayList<>();
    }

    public List<EdgeCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<EdgeCondition> conditions) {
        this.conditions = conditions != null ? conditions : new ArrayList<>();
    }

    public List<AutoAction> getAutoActions() {
        return autoActions;
    }

    public void setAutoActions(List<AutoAction> autoActions) {
        this.autoActions = autoActions != null ? autoActions : new ArrayList<>();
    }
}
