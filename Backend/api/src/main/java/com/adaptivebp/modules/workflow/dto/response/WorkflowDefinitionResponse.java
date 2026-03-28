package com.adaptivebp.modules.workflow.dto.response;

import java.util.ArrayList;
import java.util.List;

import com.adaptivebp.modules.workflow.model.WorkflowDefinition;

public class WorkflowDefinitionResponse {
    private WorkflowDefinition workflow;
    private int stepCount;
    private boolean valid;
    private List<String> validationErrors = new ArrayList<>();

    public static WorkflowDefinitionResponse of(WorkflowDefinition workflow, ValidationResult validationResult) {
        WorkflowDefinitionResponse response = new WorkflowDefinitionResponse();
        response.setWorkflow(workflow);
        response.setStepCount(workflow != null && workflow.getSteps() != null ? workflow.getSteps().size() : 0);
        if (validationResult != null) {
            response.setValid(validationResult.isValid());
            response.setValidationErrors(validationResult.getErrors());
        }
        return response;
    }

    public WorkflowDefinition getWorkflow() {
        return workflow;
    }

    public void setWorkflow(WorkflowDefinition workflow) {
        this.workflow = workflow;
    }

    public int getStepCount() {
        return stepCount;
    }

    public void setStepCount(int stepCount) {
        this.stepCount = stepCount;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<String> validationErrors) {
        this.validationErrors = validationErrors != null ? validationErrors : new ArrayList<>();
    }
}
