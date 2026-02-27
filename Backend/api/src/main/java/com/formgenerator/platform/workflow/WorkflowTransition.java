package com.formgenerator.platform.workflow;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a transition between states in a workflow.
 */
public class WorkflowTransition {
    private String id;
    private String name;
    private String fromState;
    private String toState;
    private String actionType; // SUBMIT, APPROVE, REJECT, CANCEL, PROGRESS, COMPLETE
    private String icon;
    private List<String> allowedRoles = new ArrayList<>();
    private List<TransitionCondition> conditions = new ArrayList<>();
    private List<BusinessAction> businessActions = new ArrayList<>();
    private List<String> requiredFields = new ArrayList<>();

    public WorkflowTransition() {
    }

    public WorkflowTransition(String id, String name, String fromState, String toState) {
        this.id = id;
        this.name = name;
        this.fromState = fromState;
        this.toState = toState;
    }

    // Getters and Setters
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

    public String getFromState() {
        return fromState;
    }

    public void setFromState(String fromState) {
        this.fromState = fromState;
    }

    public String getToState() {
        return toState;
    }

    public void setToState(String toState) {
        this.toState = toState;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public List<String> getAllowedRoles() {
        return allowedRoles;
    }

    public void setAllowedRoles(List<String> allowedRoles) {
        this.allowedRoles = allowedRoles;
    }

    public List<TransitionCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<TransitionCondition> conditions) {
        this.conditions = conditions;
    }

    public List<BusinessAction> getBusinessActions() {
        return businessActions;
    }

    public void setBusinessActions(List<BusinessAction> businessActions) {
        this.businessActions = businessActions;
    }

    public List<String> getRequiredFields() {
        return requiredFields;
    }

    public void setRequiredFields(List<String> requiredFields) {
        this.requiredFields = requiredFields;
    }

    /**
     * Condition that must be met for transition to be allowed
     */
    public static class TransitionCondition {
        private String type; // FIELD_REQUIRED, MIN_ITEMS, CUSTOM_VALIDATION
        private List<String> fields = new ArrayList<>();
        private Object value;

        public TransitionCondition() {
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<String> getFields() {
            return fields;
        }

        public void setFields(List<String> fields) {
            this.fields = fields;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }

    /**
     * Business action to execute when transition occurs
     */
    public static class BusinessAction {
        private String type; // VALIDATE_DATA, SEND_NOTIFICATION, ASSIGN_TO_USER, etc.
        private String description;
        private Object configuration; // Action-specific config

        public BusinessAction() {
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Object getConfiguration() {
            return configuration;
        }

        public void setConfiguration(Object configuration) {
            this.configuration = configuration;
        }
    }
}
