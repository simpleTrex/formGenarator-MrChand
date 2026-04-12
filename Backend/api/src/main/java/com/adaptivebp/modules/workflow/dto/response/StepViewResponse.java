package com.adaptivebp.modules.workflow.dto.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.adaptivebp.modules.formbuilder.model.DomainModelField;
import com.adaptivebp.modules.workflow.model.InstanceHistory;

public class StepViewResponse {
    private String instanceId;
    private String stepId;
    private String stepName;
    private List<DomainModelField> modelFields = new ArrayList<>();
    private Map<String, Object> currentData = new HashMap<>();
    private Map<String, Object> referencedData = new HashMap<>();
    private Map<String, Object> mappedData = new HashMap<>();
    private List<String> readOnlyFields = new ArrayList<>();
    private List<EdgeView> availableEdges = new ArrayList<>();
    private List<InstanceHistory> history = new ArrayList<>();

    public static class EdgeView {
        private String id;
        private String name;
        private boolean disabled;
        private String disabledReason;

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

        public boolean isDisabled() {
            return disabled;
        }

        public void setDisabled(boolean disabled) {
            this.disabled = disabled;
        }

        public String getDisabledReason() {
            return disabledReason;
        }

        public void setDisabledReason(String disabledReason) {
            this.disabledReason = disabledReason;
        }
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public List<DomainModelField> getModelFields() {
        return modelFields;
    }

    public void setModelFields(List<DomainModelField> modelFields) {
        this.modelFields = modelFields != null ? modelFields : new ArrayList<>();
    }

    public Map<String, Object> getCurrentData() {
        return currentData;
    }

    public void setCurrentData(Map<String, Object> currentData) {
        this.currentData = currentData != null ? currentData : new HashMap<>();
    }

    public Map<String, Object> getReferencedData() {
        return referencedData;
    }

    public void setReferencedData(Map<String, Object> referencedData) {
        this.referencedData = referencedData != null ? referencedData : new HashMap<>();
    }

    public Map<String, Object> getMappedData() {
        return mappedData;
    }

    public void setMappedData(Map<String, Object> mappedData) {
        this.mappedData = mappedData != null ? mappedData : new HashMap<>();
    }

    public List<String> getReadOnlyFields() {
        return readOnlyFields;
    }

    public void setReadOnlyFields(List<String> readOnlyFields) {
        this.readOnlyFields = readOnlyFields != null ? readOnlyFields : new ArrayList<>();
    }

    public List<EdgeView> getAvailableEdges() {
        return availableEdges;
    }

    public void setAvailableEdges(List<EdgeView> availableEdges) {
        this.availableEdges = availableEdges != null ? availableEdges : new ArrayList<>();
    }

    public List<InstanceHistory> getHistory() {
        return history;
    }

    public void setHistory(List<InstanceHistory> history) {
        this.history = history != null ? history : new ArrayList<>();
    }
}
