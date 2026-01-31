package com.formgenerator.platform.workflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.formgenerator.platform.workflow.WorkflowDefinitionRepository;
import com.formgenerator.platform.workflow.WorkflowDefinition;
import com.formgenerator.platform.workflow.WorkflowState;
import com.formgenerator.platform.workflow.WorkflowTransition;

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

    public void seedDefaultWorkflows(String domainId, String userId) {
        // 1. Purchase Request Approval
        WorkflowDefinition prWorkflow = new WorkflowDefinition("Purchase Request Approval", domainId,
                "purchase_request");
        prWorkflow.setDescription("Manager approval for purchase requests");
        prWorkflow.setIcon("assignment");
        prWorkflow.setCreatedBy(userId);

        java.util.List<WorkflowState> prStates = new java.util.ArrayList<>();
        prStates.add(createState("draft", "Draft", "Initial draft", true, false, "#9E9E9E", 100, 100));
        prStates.add(createState("pending_approval", "Pending Approval", "Waiting for manager", false, false, "#FF9800",
                400, 100));
        prStates.add(createState("approved", "Approved", "Request approved", false, true, "#4CAF50", 700, 50));
        prStates.add(createState("rejected", "Rejected", "Request rejected", false, true, "#F44336", 700, 200));
        prWorkflow.setStates(prStates);

        java.util.List<WorkflowTransition> prTransitions = new java.util.ArrayList<>();
        prTransitions.add(createTransition("submit", "Submit", "draft", "pending_approval", "SUBMIT", "USER"));
        prTransitions.add(createTransition("approve", "Approve", "pending_approval", "approved", "APPROVE", "MANAGER"));
        prTransitions.add(createTransition("reject", "Reject", "pending_approval", "rejected", "REJECT", "MANAGER"));
        prWorkflow.setTransitions(prTransitions);

        repository.save(prWorkflow);

        // 2. Purchase Order Processing
        WorkflowDefinition poWorkflow = new WorkflowDefinition("Purchase Order Processing", domainId, "purchase_order");
        poWorkflow.setDescription("Track PO from creation to delivery");
        poWorkflow.setIcon("shopping_cart");
        poWorkflow.setCreatedBy(userId);

        java.util.List<WorkflowState> poStates = new java.util.ArrayList<>();
        poStates.add(createState("new", "New", "New PO", true, false, "#9E9E9E", 100, 100));
        poStates.add(
                createState("sent_to_supplier", "Sent to Supplier", "PO emitted", false, false, "#2196F3", 400, 100));
        poStates.add(createState("in_progress", "In Progress", "Supplier working", false, false, "#FF9800", 700, 100));
        poStates.add(createState("completed", "Completed", "Goods received", false, true, "#4CAF50", 1000, 100));
        poWorkflow.setStates(poStates);

        java.util.List<WorkflowTransition> poTransitions = new java.util.ArrayList<>();
        poTransitions.add(createTransition("send", "Send to Supplier", "new", "sent_to_supplier", "SUBMIT", "BUYER"));
        poTransitions.add(createTransition("confirm", "Supplier Confirm", "sent_to_supplier", "in_progress", "PROGRESS",
                "BUYER"));
        poTransitions.add(createTransition("complete", "Complete", "in_progress", "completed", "COMPLETE", "SYSTEM"));
        poWorkflow.setTransitions(poTransitions);

        repository.save(poWorkflow);

        // 3. Goods Receipt
        WorkflowDefinition grWorkflow = new WorkflowDefinition("Goods Receipt Verification", domainId, "goods_receipt");
        grWorkflow.setDescription("Verify received goods quality and quantity");
        grWorkflow.setIcon("check_circle");
        grWorkflow.setCreatedBy(userId);

        java.util.List<WorkflowState> grStates = new java.util.ArrayList<>();
        grStates.add(createState("received", "Received", "Goods at dock", true, false, "#9E9E9E", 100, 100));
        grStates.add(
                createState("quality_check", "Quality Check", "Inspecting items", false, false, "#FF9800", 400, 100));
        grStates.add(createState("approved", "Approved", "Items verified", false, true, "#4CAF50", 700, 50));
        grStates.add(createState("rejected", "Rejected", "Items damaged", false, true, "#F44336", 700, 200));
        grWorkflow.setStates(grStates);

        java.util.List<WorkflowTransition> grTransitions = new java.util.ArrayList<>();
        grTransitions.add(
                createTransition("inspect", "Start Inspection", "received", "quality_check", "PROGRESS", "QC_TEAM"));
        grTransitions.add(createTransition("pass", "Pass QC", "quality_check", "approved", "APPROVE", "QC_TEAM"));
        grTransitions.add(createTransition("fail", "Fail QC", "quality_check", "rejected", "REJECT", "QC_TEAM"));
        grWorkflow.setTransitions(grTransitions);

        repository.save(grWorkflow);
    }

    private WorkflowState createState(String id, String name, String description, boolean isInitial, boolean isFinal,
            String color, double x, double y) {
        WorkflowState state = new WorkflowState();
        state.setId(id);
        state.setName(name);
        state.setDescription(description);
        state.setInitial(isInitial);
        state.setFinal(isFinal);
        state.setColor(color);
        state.setPositionX(x);
        state.setPositionY(y);
        return state;
    }

    private WorkflowTransition createTransition(String id, String name, String from, String to, String type,
            String role) {
        WorkflowTransition t = new WorkflowTransition();
        t.setId(java.util.UUID.randomUUID().toString());
        t.setName(name);
        t.setFromState(from);
        t.setToState(to);
        t.setActionType(type);
        t.setAllowedRoles(java.util.Collections.singletonList(role));
        return t;
    }
}
