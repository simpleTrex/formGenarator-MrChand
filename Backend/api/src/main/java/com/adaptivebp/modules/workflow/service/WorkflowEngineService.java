package com.adaptivebp.modules.workflow.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.adaptivebp.modules.workflow.model.WorkflowDefinition;
import com.adaptivebp.modules.workflow.model.WorkflowInstance;
import com.adaptivebp.modules.workflow.model.WorkflowState;
import com.adaptivebp.modules.workflow.model.WorkflowTransition;
import com.adaptivebp.modules.workflow.repository.WorkflowDefinitionRepository;
import com.adaptivebp.modules.workflow.repository.WorkflowInstanceRepository;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing and executing workflow instances
 */
@Service
public class WorkflowEngineService {

    @Autowired
    private WorkflowInstanceRepository instanceRepository;

    @Autowired
    private WorkflowDefinitionRepository definitionRepository;

    /**
     * Create a new workflow instance
     */
    public WorkflowInstance createInstance(String workflowDefinitionId, String domainId,
            String recordId, Map<String, Object> initialData, String createdBy) {
        Optional<WorkflowDefinition> workflowOpt = definitionRepository.findById(workflowDefinitionId);

        if (workflowOpt.isEmpty()) {
            throw new IllegalArgumentException("Workflow definition not found");
        }

        WorkflowDefinition workflow = workflowOpt.get();

        WorkflowState initialState = workflow.getInitialState();

        if (initialState == null) {
            throw new IllegalStateException("Workflow has no initial state");
        }

        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowDefinitionId(workflowDefinitionId);
        instance.setDomainId(domainId);
        instance.setModelId(workflow.getModelId());
        instance.setRecordId(recordId);
        instance.setCurrentState(initialState.getId());
        instance.setData(initialData);
        instance.setCreatedBy(createdBy);
        instance.setCreatedAt(new Date());
        instance.setUpdatedAt(new Date());

        return instanceRepository.save(instance);
    }

    /**
     * Execute a transition on a workflow instance
     */
    public WorkflowInstance executeTransition(String instanceId, String transitionId,
            String userId, String comment, Map<String, Object> additionalData) {
        Optional<WorkflowInstance> instanceOpt = instanceRepository.findById(instanceId);

        if (instanceOpt.isEmpty()) {
            throw new IllegalArgumentException("Workflow instance not found");
        }

        WorkflowInstance instance = instanceOpt.get();

        Optional<WorkflowDefinition> workflowOpt = definitionRepository.findById(instance.getWorkflowDefinitionId());

        if (workflowOpt.isEmpty()) {
            throw new IllegalStateException("Workflow definition not found");
        }

        WorkflowDefinition workflow = workflowOpt.get();

        WorkflowTransition transition = workflow.getTransitions().stream()
                .filter(t -> t.getId().equals(transitionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Transition not found"));

        if (!transition.getFromState().equals(instance.getCurrentState())) {
            throw new IllegalStateException("Invalid transition from current state");
        }

        instance.setPreviousState(instance.getCurrentState());
        instance.setCurrentState(transition.getToState());
        instance.setUpdatedAt(new Date());

        if (additionalData != null) {
            instance.getData().putAll(additionalData);
        }

        WorkflowInstance.TransitionHistory historyEntry = new WorkflowInstance.TransitionHistory();
        historyEntry.setTransitionId(transitionId);
        historyEntry.setFromState(transition.getFromState());
        historyEntry.setToState(transition.getToState());
        historyEntry.setPerformedBy(userId);
        historyEntry.setPerformedAt(new Date());
        historyEntry.setComment(comment);

        instance.getHistory().add(historyEntry);

        return instanceRepository.save(instance);
    }

    /**
     * Get available transitions from current state for a workflow instance
     */
    public List<WorkflowTransition> getAvailableTransitions(String instanceId) {
        Optional<WorkflowInstance> instanceOpt = instanceRepository.findById(instanceId);

        if (instanceOpt.isEmpty()) {
            throw new IllegalArgumentException("Workflow instance not found");
        }

        WorkflowInstance instance = instanceOpt.get();

        Optional<WorkflowDefinition> workflowOpt = definitionRepository.findById(instance.getWorkflowDefinitionId());

        if (workflowOpt.isEmpty()) {
            throw new IllegalStateException("Workflow definition not found");
        }

        WorkflowDefinition workflow = workflowOpt.get();

        return workflow.getTransitionsFromState(instance.getCurrentState());
    }

    /**
     * Get workflow instance by ID
     */
    public Optional<WorkflowInstance> getInstance(String instanceId) {
        return instanceRepository.findById(instanceId);
    }

    /**
     * Get all instances for a workflow definition
     */
    public List<WorkflowInstance> getInstancesByWorkflow(String workflowDefinitionId) {
        return instanceRepository.findByWorkflowDefinitionId(workflowDefinitionId);
    }

    /**
     * Get instances by current state
     */
    public List<WorkflowInstance> getInstancesByState(String domainId, String state) {
        return instanceRepository.findByDomainIdAndCurrentState(domainId, state);
    }

    /**
     * Get instances assigned to a user
     */
    public List<WorkflowInstance> getInstancesByUser(String userId) {
        return instanceRepository.findByAssignedTo_UserId(userId);
    }

    /**
     * Add comment to workflow instance
     */
    public WorkflowInstance addComment(String instanceId, String userId, String text) {
        Optional<WorkflowInstance> instanceOpt = instanceRepository.findById(instanceId);

        if (instanceOpt.isEmpty()) {
            throw new IllegalArgumentException("Workflow instance not found");
        }

        WorkflowInstance instance = instanceOpt.get();

        WorkflowInstance.Comment commentObj = new WorkflowInstance.Comment();
        commentObj.setId(UUID.randomUUID().toString());
        commentObj.setUserId(userId);
        commentObj.setText(text);
        commentObj.setCreatedAt(new Date());

        instance.getComments().add(commentObj);
        instance.setUpdatedAt(new Date());

        return instanceRepository.save(instance);
    }

    /**
     * Assign workflow instance to a user
     */
    public WorkflowInstance assignToUser(String instanceId, String userId, String role) {
        Optional<WorkflowInstance> instanceOpt = instanceRepository.findById(instanceId);

        if (instanceOpt.isEmpty()) {
            throw new IllegalArgumentException("Workflow instance not found");
        }

        WorkflowInstance instance = instanceOpt.get();

        WorkflowInstance.AssignmentInfo assignment = new WorkflowInstance.AssignmentInfo();
        assignment.setUserId(userId);
        assignment.setRole(role);
        assignment.setAssignedAt(new Date());

        instance.setAssignedTo(assignment);
        instance.setUpdatedAt(new Date());

        return instanceRepository.save(instance);
    }
}
