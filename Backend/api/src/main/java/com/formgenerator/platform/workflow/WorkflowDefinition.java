package com.formgenerator.platform.workflow;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * WorkflowDefinition represents a workflow template that can be applied to data models.
 * It defines the state machine, transitions, and business rules for a business process.
 */
@Document(collection = "workflow_definitions")
public class WorkflowDefinition {
    @Id
    private String id;
    
    @Indexed
    private String domainId;
    
    @NotBlank
    @Size(min = 3, max = 100)
    private String name;
    
    private String description;
    
    // Reference to the data model this workflow applies to
    @Indexed
    private String modelId;
    
    private String icon;
    
    // List of states in this workflow
    private List<WorkflowState> states = new ArrayList<>();
    
    // List of transitions between states
    private List<WorkflowTransition> transitions = new ArrayList<>();
    
    private Date createdAt;
    private Date updatedAt;
    private String createdBy;
    private int version = 1;
    private boolean isActive = true;
    
    public WorkflowDefinition() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }
    
    public WorkflowDefinition(String name, String domainId, String modelId) {
        this.name = name;
        this.domainId = domainId;
        this.modelId = modelId;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getDomainId() {
        return domainId;
    }
    
    public void setDomainId(String domainId) {
        this.domainId = domainId;
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
    
    public String getModelId() {
        return modelId;
    }
    
    public void setModelId(String modelId) {
        this.modelId = modelId;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public List<WorkflowState> getStates() {
        return states;
    }
    
    public void setStates(List<WorkflowState> states) {
        this.states = states;
    }
    
    public List<WorkflowTransition> getTransitions() {
        return transitions;
    }
    
    public void setTransitions(List<WorkflowTransition> transitions) {
        this.transitions = transitions;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public int getVersion() {
        return version;
    }
    
    public void setVersion(int version) {
        this.version = version;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    /**
     * Get the initial state of this workflow
     */
    public WorkflowState getInitialState() {
        return states.stream()
                .filter(WorkflowState::isInitial)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Find a state by its ID
     */
    public WorkflowState getStateById(String stateId) {
        return states.stream()
                .filter(s -> s.getId().equals(stateId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Get all transitions from a specific state
     */
    public List<WorkflowTransition> getTransitionsFromState(String stateId) {
        List<WorkflowTransition> result = new ArrayList<>();
        for (WorkflowTransition transition : transitions) {
            if (transition.getFromState().equals(stateId)) {
                result.add(transition);
            }
        }
        return result;
    }
}
