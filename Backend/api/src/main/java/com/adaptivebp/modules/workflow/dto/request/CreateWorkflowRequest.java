package com.adaptivebp.modules.workflow.dto.request;

import java.util.ArrayList;
import java.util.List;

import com.adaptivebp.modules.workflow.model.WorkflowEdge;
import com.adaptivebp.modules.workflow.model.WorkflowStep;

import jakarta.validation.constraints.NotBlank;

public class CreateWorkflowRequest {
    @NotBlank
    private String name;
    private String description;
    private List<WorkflowStep> steps = new ArrayList<>();
    private List<WorkflowEdge> globalEdges = new ArrayList<>();

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

    public List<WorkflowStep> getSteps() {
        return steps;
    }

    public void setSteps(List<WorkflowStep> steps) {
        this.steps = steps != null ? steps : new ArrayList<>();
    }

    public List<WorkflowEdge> getGlobalEdges() {
        return globalEdges;
    }

    public void setGlobalEdges(List<WorkflowEdge> globalEdges) {
        this.globalEdges = globalEdges != null ? globalEdges : new ArrayList<>();
    }
}
