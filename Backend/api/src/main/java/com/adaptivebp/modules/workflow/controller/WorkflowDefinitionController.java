package com.adaptivebp.modules.workflow.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.adaptivebp.modules.appmanagement.model.Application;
import com.adaptivebp.modules.appmanagement.permission.AppPermission;
import com.adaptivebp.modules.appmanagement.port.ApplicationLookupPort;
import com.adaptivebp.modules.organisation.model.Organisation;
import com.adaptivebp.modules.organisation.permission.DomainPermission;
import com.adaptivebp.modules.organisation.port.OrganisationLookupPort;
import com.adaptivebp.modules.organisation.service.PermissionService;
import com.adaptivebp.modules.workflow.dto.request.CreateWorkflowRequest;
import com.adaptivebp.modules.workflow.dto.request.UpdateWorkflowRequest;
import com.adaptivebp.modules.workflow.dto.response.WorkflowDefinitionResponse;
import com.adaptivebp.modules.workflow.model.WorkflowDefinition;
import com.adaptivebp.modules.workflow.service.WorkflowDefinitionService;
import com.adaptivebp.modules.workflow.service.WorkflowValidationService;
import com.adaptivebp.shared.security.AdaptiveUserDetails;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/adaptive/domains/{slug}/apps/{appSlug}/workflows")
public class WorkflowDefinitionController {

    @Autowired
    private OrganisationLookupPort organisationLookupPort;

    @Autowired
    private ApplicationLookupPort applicationLookupPort;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private WorkflowDefinitionService definitionService;

    @Autowired
    private WorkflowValidationService validationService;

    @PostMapping
    public ResponseEntity<WorkflowDefinitionResponse> createWorkflow(
            @PathVariable String slug,
            @PathVariable String appSlug,
            @Valid @RequestBody CreateWorkflowRequest request) {
        Context ctx = resolve(slug, appSlug);
        requireDefinitionManagePermission(ctx);

        WorkflowDefinition created = definitionService.createWorkflow(
                ctx.domain().getId(),
                ctx.app().getId(),
                request,
                currentUserId());

        return ResponseEntity.ok(WorkflowDefinitionResponse.of(created, validationService.validate(created)));
    }

    @GetMapping
    public ResponseEntity<List<WorkflowDefinition>> listWorkflows(
            @PathVariable String slug,
            @PathVariable String appSlug) {
        Context ctx = resolve(slug, appSlug);
        requireViewPermission(ctx.app().getId());
        return ResponseEntity.ok(definitionService.listWorkflows(ctx.domain().getId(), ctx.app().getId()));
    }

    @GetMapping("/{wfSlug}")
    public ResponseEntity<WorkflowDefinitionResponse> getWorkflow(
            @PathVariable String slug,
            @PathVariable String appSlug,
            @PathVariable String wfSlug,
            @RequestParam(required = false) Integer version) {
        Context ctx = resolve(slug, appSlug);
        requireViewPermission(ctx.app().getId());

        WorkflowDefinition workflow = definitionService.getWorkflow(
                ctx.domain().getId(),
                ctx.app().getId(),
                wfSlug,
                version);

        return ResponseEntity.ok(WorkflowDefinitionResponse.of(workflow, validationService.validate(workflow)));
    }

    @PutMapping("/{wfSlug}")
    public ResponseEntity<WorkflowDefinitionResponse> updateWorkflow(
            @PathVariable String slug,
            @PathVariable String appSlug,
            @PathVariable String wfSlug,
            @RequestBody UpdateWorkflowRequest request) {
        Context ctx = resolve(slug, appSlug);
        requireDefinitionManagePermission(ctx);

        WorkflowDefinition updated = definitionService.updateWorkflow(
                ctx.domain().getId(),
                ctx.app().getId(),
                wfSlug,
                request);

        return ResponseEntity.ok(WorkflowDefinitionResponse.of(updated, validationService.validate(updated)));
    }

    @DeleteMapping("/{wfSlug}")
    public ResponseEntity<Void> deleteWorkflow(
            @PathVariable String slug,
            @PathVariable String appSlug,
            @PathVariable String wfSlug) {
        Context ctx = resolve(slug, appSlug);
        requireDefinitionManagePermission(ctx);

        definitionService.deleteWorkflow(ctx.domain().getId(), ctx.app().getId(), wfSlug);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{wfSlug}/publish")
    public ResponseEntity<WorkflowDefinitionResponse> publishWorkflow(
            @PathVariable String slug,
            @PathVariable String appSlug,
            @PathVariable String wfSlug) {
        Context ctx = resolve(slug, appSlug);
        requireDefinitionManagePermission(ctx);

        WorkflowDefinition published = definitionService.publishWorkflow(ctx.domain().getId(), ctx.app().getId(), wfSlug);
        return ResponseEntity.ok(WorkflowDefinitionResponse.of(published, validationService.validate(published)));
    }

    @PostMapping("/{wfSlug}/archive")
    public ResponseEntity<WorkflowDefinition> archiveWorkflow(
            @PathVariable String slug,
            @PathVariable String appSlug,
            @PathVariable String wfSlug) {
        Context ctx = resolve(slug, appSlug);
        requireDefinitionManagePermission(ctx);

        WorkflowDefinition archived = definitionService.archiveWorkflow(ctx.domain().getId(), ctx.app().getId(), wfSlug);
        return ResponseEntity.ok(archived);
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

    private void requireDefinitionManagePermission(Context ctx) {
        boolean hasDomainPermission = permissionService.hasDomainPermission(
                ctx.domain().getId(),
                DomainPermission.DOMAIN_MANAGE_WORKFLOWS);
        boolean hasAppPermission = permissionService.hasAppPermission(
                ctx.app().getId(),
                AppPermission.APP_MANAGE_WORKFLOWS);

        if (!hasDomainPermission && !hasAppPermission) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        }
    }

    private void requireViewPermission(String appId) {
        if (!permissionService.hasAppPermission(appId, AppPermission.APP_VIEW_WORKFLOWS)) {
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
}
