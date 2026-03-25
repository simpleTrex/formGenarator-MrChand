package com.adaptivebp.modules.process.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.adaptivebp.modules.organisation.model.Organisation;
import com.adaptivebp.modules.organisation.port.OrganisationLookupPort;
import com.adaptivebp.modules.organisation.service.PermissionService;
import com.adaptivebp.modules.process.dto.NodeViewResponse;
import com.adaptivebp.modules.process.dto.ProcessInstanceResponse;
import com.adaptivebp.modules.process.dto.SubmitNodeRequest;
import com.adaptivebp.modules.process.model.ProcessInstance;
import com.adaptivebp.modules.process.service.ProcessEngineService;
import com.adaptivebp.shared.security.AdaptiveUserDetails;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

@RestController
@RequestMapping("/adaptive/domains/{slug}/apps/{appSlug}")
public class ProcessInstanceController {

    @Autowired
    private OrganisationLookupPort organisationLookupPort;

    @Autowired
    private ApplicationLookupPort applicationLookupPort;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ProcessEngineService engineService;

    // ── Runtime endpoints ─────────────────────────────────────────────────────

    // Any authenticated domain user can start a specific process
    @PostMapping("/processes/{processSlug}/instances/start")
    public ResponseEntity<?> startProcess(@PathVariable String slug, @PathVariable String appSlug, @PathVariable String processSlug) {
        Context ctx = resolve(slug, appSlug);
        requireAuthenticated();

        String userId = currentUserId();
        ProcessInstanceResponse response = engineService.startProcess(
                ctx.domain().getId(), ctx.app().getId(), processSlug, userId);
        return ResponseEntity.ok(response);
    }

    // Only users with APP_VIEW_PROCESSES (admins/owners) can see all instances
    @GetMapping("/instances")
    public ResponseEntity<?> listInstances(@PathVariable String slug, @PathVariable String appSlug) {
        Context ctx = resolve(slug, appSlug);
        requireAppPermission(ctx.app().getId(), AppPermission.APP_VIEW_PROCESSES);

        List<ProcessInstance> instances = engineService.listInstances(ctx.domain().getId(), ctx.app().getId());
        return ResponseEntity.ok(instances);
    }

    // Any authenticated user can view their own instance
    @GetMapping("/instances/{instanceId}")
    public ResponseEntity<?> getInstance(@PathVariable String slug, @PathVariable String appSlug,
            @PathVariable String instanceId) {
        resolve(slug, appSlug);
        requireAuthenticated();

        ProcessInstance instance = engineService.getInstance(instanceId);
        return ResponseEntity.ok(instance);
    }

    // Any authenticated user can view the current node of their instance
    @GetMapping("/instances/{instanceId}/current-node")
    public ResponseEntity<?> getNodeView(@PathVariable String slug, @PathVariable String appSlug,
            @PathVariable String instanceId) {
        resolve(slug, appSlug);
        requireAuthenticated();

        String userId = currentUserId();
        NodeViewResponse view = engineService.getNodeView(instanceId, userId);
        return ResponseEntity.ok(view);
    }

    // Any authenticated user can submit their own node
    @PostMapping("/instances/{instanceId}/submit")
    public ResponseEntity<?> submitNode(@PathVariable String slug, @PathVariable String appSlug,
            @PathVariable String instanceId, @Valid @RequestBody SubmitNodeRequest request) {
        resolve(slug, appSlug);
        requireAuthenticated();

        String userId = currentUserId();
        ProcessInstanceResponse response = engineService.submitNode(
                instanceId,
                request.getNodeId(),
                request.getFormData(),
                request.getAction(),
                request.getComment(),
                userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/instances/{instanceId}/save-draft")
    public ResponseEntity<?> saveDraft(@PathVariable String slug, @PathVariable String appSlug,
            @PathVariable String instanceId, @RequestBody Map<String, Object> body) {
        resolve(slug, appSlug);
        requireAuthenticated();

        String nodeId = (String) body.get("nodeId");
        @SuppressWarnings("unchecked")
        Map<String, Object> partialData = (Map<String, Object>) body.get("data");
        String userId = currentUserId();

        ProcessInstance instance = engineService.saveDraft(instanceId, nodeId, partialData, userId);
        return ResponseEntity.ok(instance);
    }

    @PostMapping("/instances/{instanceId}/cancel")
    public ResponseEntity<?> cancelInstance(@PathVariable String slug, @PathVariable String appSlug,
            @PathVariable String instanceId) {
        resolve(slug, appSlug);
        requireAuthenticated();

        String userId = currentUserId();
        ProcessInstance instance = engineService.cancelInstance(instanceId, userId);
        return ResponseEntity.ok(instance);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private record Context(Organisation domain, Application app) {}

    private Context resolve(String slug, String appSlug) {
        Organisation domain = organisationLookupPort.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found"));
        Application app = applicationLookupPort.findByDomainIdAndSlug(domain.getId(), appSlug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        return new Context(domain, app);
    }

    private Application resolveApp(String slug, String appSlug) {
        Organisation domain = organisationLookupPort.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found"));
        return applicationLookupPort.findByDomainIdAndSlug(domain.getId(), appSlug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    private void requireAppPermission(String appId, AppPermission permission) {
        if (!permissionService.hasAppPermission(appId, permission)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        }
    }

    private void requireAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AdaptiveUserDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
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
