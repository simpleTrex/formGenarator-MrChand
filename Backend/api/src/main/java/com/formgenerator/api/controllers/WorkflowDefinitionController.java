package com.formgenerator.api.controllers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.formgenerator.platform.auth.AdaptiveUserDetails;
import com.formgenerator.platform.auth.MessageResponse;
import com.formgenerator.platform.workflow.*;

import jakarta.validation.Valid;

/**
 * REST Controller for Workflow Definition management
 */
@RestController
@RequestMapping("/custom_form/workflows")
public class WorkflowDefinitionController {

    @Autowired
    private WorkflowDefinitionService workflowService;

    @Autowired
    private WorkflowEngineService engineService;

    /**
     * Create a new workflow definition
     */
    @PostMapping("")
    public ResponseEntity<?> createWorkflow(@Valid @RequestBody WorkflowDefinition workflow) {
        System.out.println("DEBUG: WorkflowDefinitionController - createWorkflow hit");
        try {
            AdaptiveUserDetails principal = currentAdaptivePrincipal();
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // For OWNER users, use domainId from the workflow object
            // For DOMAIN_USER, use domainId from the token
            String domainId;
            if (principal.getDomainId() != null) {
                domainId = principal.getDomainId();
            } else if (workflow.getDomainId() != null) {
                // OWNER can specify domainId in the request
                domainId = workflow.getDomainId();
            } else {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: domainId is required"));
            }

            workflow.setDomainId(domainId);
            workflow.setCreatedBy(principal.getId());

            WorkflowDefinition created = workflowService.createWorkflow(workflow);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Get all workflows for current domain
     */
    /**
     * Get all workflows for current domain
     */
    @GetMapping("")
    public ResponseEntity<?> getWorkflows(@RequestParam(required = false) String domainId) {
        AdaptiveUserDetails principal = currentAdaptivePrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String effectiveDomainId;
        if (principal.getDomainId() != null) {
            effectiveDomainId = principal.getDomainId();
        } else if (domainId != null) {
            // OWNER provided domainId via param
            effectiveDomainId = domainId;
        } else {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: domainId query parameter is required for OWNER"));
        }

        List<WorkflowDefinition> workflows = workflowService.getWorkflowsByDomain(effectiveDomainId);
        return ResponseEntity.ok(workflows);
    }

    /**
     * Get a specific workflow by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getWorkflow(@PathVariable String id, @RequestParam(required = false) String domainId) {
        AdaptiveUserDetails principal = currentAdaptivePrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String effectiveDomainId;
        if (principal.getDomainId() != null) {
            effectiveDomainId = principal.getDomainId();
        } else if (domainId != null) {
            effectiveDomainId = domainId;
        } else {
            // For getById, we might allow OWNER to fetch without domainId if the service
            // supports it?
            // But existing service method requires domainId.
            return ResponseEntity.badRequest().body(new MessageResponse("Error: domainId is required"));
        }

        Optional<WorkflowDefinition> workflow = workflowService.getWorkflowById(id, effectiveDomainId);

        if (workflow.isPresent()) {
            return ResponseEntity.ok(workflow.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update a workflow definition
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateWorkflow(@PathVariable String id, @Valid @RequestBody WorkflowDefinition workflow) {
        try {
            AdaptiveUserDetails principal = currentAdaptivePrincipal();
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String effectiveDomainId;
            if (principal.getDomainId() != null) {
                effectiveDomainId = principal.getDomainId();
            } else if (workflow.getDomainId() != null) {
                effectiveDomainId = workflow.getDomainId();
            } else {
                return ResponseEntity.badRequest().body(new MessageResponse("Error: domainId is required"));
            }

            WorkflowDefinition updated = workflowService.updateWorkflow(id, effectiveDomainId, workflow);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Delete a workflow definition
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWorkflow(@PathVariable String id, @RequestParam(required = false) String domainId) {
        try {
            AdaptiveUserDetails principal = currentAdaptivePrincipal();
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String effectiveDomainId;
            if (principal.getDomainId() != null) {
                effectiveDomainId = principal.getDomainId();
            } else if (domainId != null) {
                effectiveDomainId = domainId;
            } else {
                return ResponseEntity.badRequest().body(new MessageResponse("Error: domainId is required"));
            }

            workflowService.deleteWorkflow(id, effectiveDomainId);
            return ResponseEntity.ok(new MessageResponse("Workflow deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Create a new workflow instance
     */
    @PostMapping("/{workflowId}/instances")
    public ResponseEntity<?> createInstance(@PathVariable String workflowId,
            @RequestBody Map<String, Object> request) {
        try {
            AdaptiveUserDetails principal = currentAdaptivePrincipal();
            if (principal == null || principal.getDomainId() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String recordId = (String) request.get("recordId");
            @SuppressWarnings("unchecked")
            Map<String, Object> initialData = (Map<String, Object>) request.get("data");

            WorkflowInstance instance = engineService.createInstance(
                    workflowId,
                    principal.getDomainId(),
                    recordId,
                    initialData != null ? initialData : Map.of(),
                    principal.getId());

            return ResponseEntity.ok(instance);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Execute a transition on a workflow instance
     */
    @PostMapping("/instances/{instanceId}/transitions/{transitionId}")
    public ResponseEntity<?> executeTransition(@PathVariable String instanceId,
            @PathVariable String transitionId,
            @RequestBody(required = false) Map<String, Object> request) {
        try {
            AdaptiveUserDetails principal = currentAdaptivePrincipal();
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String comment = request != null ? (String) request.get("comment") : null;
            @SuppressWarnings("unchecked")
            Map<String, Object> additionalData = request != null ? (Map<String, Object>) request.get("data") : null;

            WorkflowInstance instance = engineService.executeTransition(
                    instanceId,
                    transitionId,
                    principal.getId(),
                    comment,
                    additionalData);

            return ResponseEntity.ok(instance);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Get available transitions for a workflow instance
     */
    @GetMapping("/instances/{instanceId}/actions")
    public ResponseEntity<?> getAvailableActions(@PathVariable String instanceId) {
        try {
            List<WorkflowTransition> transitions = engineService.getAvailableTransitions(instanceId);
            return ResponseEntity.ok(Map.of("availableTransitions", transitions));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Get a workflow instance
     */
    @GetMapping("/instances/{instanceId}")
    public ResponseEntity<?> getInstance(@PathVariable String instanceId) {
        Optional<WorkflowInstance> instance = engineService.getInstance(instanceId);

        if (instance.isPresent()) {
            return ResponseEntity.ok(instance.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get instances for current user
     */
    @GetMapping("/instances/my-tasks")
    public ResponseEntity<?> getMyTasks() {
        AdaptiveUserDetails principal = currentAdaptivePrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<WorkflowInstance> instances = engineService.getInstancesByUser(principal.getId());
        return ResponseEntity.ok(instances);
    }

    /**
     * Add comment to workflow instance
     */
    @PostMapping("/instances/{instanceId}/comments")
    public ResponseEntity<?> addComment(@PathVariable String instanceId, @RequestBody Map<String, String> request) {
        try {
            AdaptiveUserDetails principal = currentAdaptivePrincipal();
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String text = request.get("text");
            WorkflowInstance instance = engineService.addComment(instanceId, principal.getId(), text);
            return ResponseEntity.ok(instance);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    // Helper methods
    private AdaptiveUserDetails currentAdaptivePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AdaptiveUserDetails details) {
            return details;
        }
        return null;
    }
}
