package com.adaptivebp.modules.workflow.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.adaptivebp.modules.appmanagement.model.Application;
import com.adaptivebp.modules.appmanagement.permission.AppPermission;
import com.adaptivebp.modules.appmanagement.port.ApplicationLookupPort;
import com.adaptivebp.modules.organisation.permission.DomainPermission;
import com.adaptivebp.modules.organisation.model.Organisation;
import com.adaptivebp.modules.organisation.port.OrganisationLookupPort;
import com.adaptivebp.modules.organisation.service.PermissionService;
import com.adaptivebp.modules.workflow.dto.request.ExecuteEdgeRequest;
import com.adaptivebp.modules.workflow.dto.request.StartWorkflowRequest;
import com.adaptivebp.modules.workflow.dto.response.ExecuteEdgeResponse;
import com.adaptivebp.modules.workflow.dto.response.HistoryResponse;
import com.adaptivebp.modules.workflow.dto.response.StepViewResponse;
import com.adaptivebp.modules.workflow.dto.response.TaskListResponse;
import com.adaptivebp.modules.workflow.dto.response.WorkflowInstanceResponse;
import com.adaptivebp.modules.workflow.model.WorkflowInstance;
import com.adaptivebp.modules.workflow.service.WorkflowEngineService;
import com.adaptivebp.modules.workflow.service.WorkflowTaskService;
import com.adaptivebp.shared.security.AdaptiveUserDetails;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/adaptive/domains/{slug}/apps/{appSlug}")
public class WorkflowInstanceController {

    @Autowired
    private OrganisationLookupPort organisationLookupPort;

    @Autowired
    private ApplicationLookupPort applicationLookupPort;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private WorkflowEngineService engineService;

    @Autowired
    private WorkflowTaskService taskService;

    @PostMapping("/workflows/{wfSlug}/start")
    public ResponseEntity<WorkflowInstanceResponse> startWorkflow(
            @PathVariable String slug,
            @PathVariable String appSlug,
            @PathVariable String wfSlug,
            @RequestBody(required = false) StartWorkflowRequest request) {
        Context ctx = resolve(slug, appSlug);
        requireAppPermission(ctx.app().getId(), AppPermission.APP_START_WORKFLOW);

        WorkflowInstance instance = engineService.startWorkflowBySlug(
                ctx.domain().getId(),
                ctx.app().getId(),
                wfSlug,
                request != null ? request.getFormData() : null,
                currentUserId(),
                currentUsername());

        return ResponseEntity.ok(WorkflowInstanceResponse.from(instance));
    }

    @GetMapping("/instances")
    public ResponseEntity<List<WorkflowInstance>> listInstances(
            @PathVariable String slug,
            @PathVariable String appSlug) {
        Context ctx = resolve(slug, appSlug);
        requireAppPermission(ctx.app().getId(), AppPermission.APP_VIEW_ALL_INSTANCES);
        return ResponseEntity.ok(engineService.listInstances(ctx.domain().getId(), ctx.app().getId()));
    }

    @GetMapping("/instances/my-tasks")
    public ResponseEntity<TaskListResponse> getMyTasks(
            @PathVariable String slug,
            @PathVariable String appSlug) {
        Context ctx = resolve(slug, appSlug);
        requireAuthenticated();
        return ResponseEntity.ok(taskService.getMyTasks(currentUserId(), ctx.domain().getId(), ctx.app().getId()));
    }

    @GetMapping("/instances/my-started")
    public ResponseEntity<List<WorkflowInstance>> getMyStartedInstances(
            @PathVariable String slug,
            @PathVariable String appSlug) {
        Context ctx = resolve(slug, appSlug);
        requireAuthenticated();
        return ResponseEntity.ok(engineService.listStartedBy(ctx.domain().getId(), ctx.app().getId(), currentUserId()));
    }

    @GetMapping("/instances/{instanceId}")
    public ResponseEntity<WorkflowInstance> getInstance(
            @PathVariable String slug,
            @PathVariable String appSlug,
            @PathVariable String instanceId) {
        Context ctx = resolve(slug, appSlug);
        requireAuthenticated();
        WorkflowInstance instance = engineService.getInstance(instanceId);
        // Allow access if user has APP_VIEW_ALL_INSTANCES (admin/supervisor)
        // OR if the user is the one who started this specific instance (submitter)
        String uid = currentUserId();
        boolean hasAllInstances = permissionService.hasAppPermission(ctx.app().getId(), AppPermission.APP_VIEW_ALL_INSTANCES);
        boolean isSubmitter = uid != null && uid.equals(instance.getStartedBy());
        if (!hasAllInstances && !isSubmitter) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(instance);
    }

    @GetMapping("/instances/{instanceId}/view")
    public ResponseEntity<StepViewResponse> getStepView(
            @PathVariable String slug,
            @PathVariable String appSlug,
            @PathVariable String instanceId) {
        resolve(slug, appSlug);
        requireAuthenticated();
        return ResponseEntity.ok(engineService.getStepView(instanceId, currentUserId()));
    }

    @PostMapping("/instances/{instanceId}/execute")
    public ResponseEntity<ExecuteEdgeResponse> executeEdge(
            @PathVariable String slug,
            @PathVariable String appSlug,
            @PathVariable String instanceId,
            @Valid @RequestBody ExecuteEdgeRequest request) {
        Context ctx = resolve(slug, appSlug);
        // Allow any domain member to attempt execution.
        // Edge-level authorization (workflow roles, onlySubmitter, allowedUserIds)
        // is enforced inside WorkflowEngineService.executeEdge().
        if (!permissionService.hasAppPermission(ctx.app().getId(), AppPermission.APP_EXECUTE_WORKFLOW)
                && !permissionService.hasDomainPermission(ctx.domain().getId(), DomainPermission.DOMAIN_USE_APP)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        }

        return ResponseEntity.ok(engineService.executeEdge(
                instanceId,
                request.getEdgeId(),
                request.getFormData(),
                request.getComment(),
                currentUserId(),
                currentUsername()));
    }

    @GetMapping("/instances/{instanceId}/history")
    public ResponseEntity<HistoryResponse> getHistory(
            @PathVariable String slug,
            @PathVariable String appSlug,
            @PathVariable String instanceId) {
        resolve(slug, appSlug);
        requireAuthenticated();
        return ResponseEntity.ok(engineService.getHistory(instanceId));
    }

    private record Context(Organisation domain, Application app) {
    }

    private Context resolve(String slug, String appSlug) {
        Organisation domain = organisationLookupPort.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found"));
        Application app = applicationLookupPort.findByDomainIdAndSlug(domain.getId(), appSlug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        return new Context(domain, app);
    }

    private void requireAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AdaptiveUserDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }

    private void requireAppPermission(String appId, AppPermission permission) {
        if (!permissionService.hasAppPermission(appId, permission)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        }
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AdaptiveUserDetails details) {
            return details.getId();
        }
        return null;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AdaptiveUserDetails details) {
            return details.getUsername();
        }
        return currentUserId();
    }
}
