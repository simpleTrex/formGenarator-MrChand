package com.formgenerator.platform.workflow;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a state in a workflow state machine.
 */
public class WorkflowState {
    private String id;
    private String name;
    private String description;

    @JsonProperty("isInitial")
    private boolean isInitial;

    @JsonProperty("isFinal")
    private boolean isFinal;
    private String color;

    // Visual designer properties
    private double positionX;
    private double positionY;

    // Permissions per role for this state
    private Map<String, StatePermissions> permissions = new HashMap<>();

    public WorkflowState() {
    }

    public WorkflowState(String id, String name) {
        this.id = id;
        this.name = name;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isInitial() {
        return isInitial;
    }

    public void setInitial(boolean initial) {
        isInitial = initial;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public double getPositionX() {
        return positionX;
    }

    public void setPositionX(double positionX) {
        this.positionX = positionX;
    }

    public double getPositionY() {
        return positionY;
    }

    public void setPositionY(double positionY) {
        this.positionY = positionY;
    }

    public Map<String, StatePermissions> getPermissions() {
        return permissions;
    }

    public void setPermissions(Map<String, StatePermissions> permissions) {
        this.permissions = permissions;
    }

    public static class StatePermissions {
        private boolean canView = true;
        private boolean canEdit = false;
        private boolean canDelete = false;

        public StatePermissions() {
        }

        public boolean isCanView() {
            return canView;
        }

        public void setCanView(boolean canView) {
            this.canView = canView;
        }

        public boolean isCanEdit() {
            return canEdit;
        }

        public void setCanEdit(boolean canEdit) {
            this.canEdit = canEdit;
        }

        public boolean isCanDelete() {
            return canDelete;
        }

        public void setCanDelete(boolean canDelete) {
            this.canDelete = canDelete;
        }
    }
}
