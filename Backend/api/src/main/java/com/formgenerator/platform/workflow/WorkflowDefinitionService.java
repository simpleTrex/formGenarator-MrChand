package com.formgenerator.platform.workflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing workflow definitions
 */
@Service
public class WorkflowDefinitionService {

    @Autowired
    private WorkflowDefinitionRepository repository;

    /**
     * Create a new workflow definition
     */
    public WorkflowDefinition createWorkflow(WorkflowDefinition workflow) {
        workflow.setCreatedAt(new Date());
        workflow.setUpdatedAt(new Date());
        workflow.setVersion(1);
        workflow.setActive(true);

        // Validate workflow has at least one initial state
        long initialStates = workflow.getStates().stream()
                .filter(WorkflowState::isInitial)
                .count();

        if (initialStates == 0) {
            throw new IllegalArgumentException("Workflow must have at least one initial state");
        }

        if (initialStates > 1) {
            throw new IllegalArgumentException("Workflow can only have one initial state");
        }

        return repository.save(workflow);
    }

    /**
     * Update an existing workflow definition
     */
    public WorkflowDefinition updateWorkflow(String id, String domainId, WorkflowDefinition updatedWorkflow) {
        Optional<WorkflowDefinition> existing = repository.findByIdAndDomainId(id, domainId);

        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Workflow not found");
        }

        WorkflowDefinition workflow = existing.get();
        workflow.setName(updatedWorkflow.getName());
        workflow.setDescription(updatedWorkflow.getDescription());
        workflow.setStates(updatedWorkflow.getStates());
        workflow.setTransitions(updatedWorkflow.getTransitions());
        workflow.setIcon(updatedWorkflow.getIcon());
        workflow.setUpdatedAt(new Date());
        workflow.setVersion(workflow.getVersion() + 1);

        return repository.save(workflow);
    }

    /**
     * Get all workflows for a domain
     */
    public List<WorkflowDefinition> getWorkflowsByDomain(String domainId) {
        return repository.findByDomainId(domainId);
    }

    /**
     * Get active workflows for a domain
     */
    public List<WorkflowDefinition> getActiveWorkflows(String domainId) {
        return repository.findByDomainIdAndIsActive(domainId, true);
    }

    /**
     * Get a specific workflow by ID
     */
    public Optional<WorkflowDefinition> getWorkflowById(String id, String domainId) {
        return repository.findByIdAndDomainId(id, domainId);
    }

    /**
     * Delete a workflow definition
     */
    public void deleteWorkflow(String id, String domainId) {
        Optional<WorkflowDefinition> workflow = repository.findByIdAndDomainId(id, domainId);

        if (workflow.isPresent()) {
            repository.delete(workflow.get());
        } else {
            throw new IllegalArgumentException("Workflow not found");
        }
    }

    /**
     * Deactivate a workflow (soft delete)
     */
    public void deactivateWorkflow(String id, String domainId) {
        Optional<WorkflowDefinition> workflow = repository.findByIdAndDomainId(id, domainId);

        if (workflow.isPresent()) {
            WorkflowDefinition wf = workflow.get();
            wf.setActive(false);
            wf.setUpdatedAt(new Date());
            repository.save(wf);
        } else {
            throw new IllegalArgumentException("Workflow not found");
        }
    }

    /**
     * Get workflows associated with a specific model
     */
    public List<WorkflowDefinition> getWorkflowsByModel(String modelId) {
        return repository.findByModelId(modelId);
    }

    /**
     * Validate if a transition is valid from the current state
     */
    public boolean isValidTransition(WorkflowDefinition workflow, String fromState, String transitionId) {
        return workflow.getTransitions().stream()
                .anyMatch(t -> t.getId().equals(transitionId) && t.getFromState().equals(fromState));
    }
}
