package com.adaptivebp.modules.process.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
import com.adaptivebp.modules.process.dto.CreateProcessRequest;
import com.adaptivebp.modules.process.dto.ProcessDefinitionResponse;
import com.adaptivebp.modules.process.dto.UpdateProcessRequest;
import com.adaptivebp.modules.process.dto.ValidationResult;
import com.adaptivebp.modules.process.model.ProcessDefinition;
import com.adaptivebp.modules.process.service.ProcessDefinitionService;
import com.adaptivebp.modules.process.service.ProcessValidationService;
import com.adaptivebp.shared.security.AdaptiveUserDetails;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Each application has exactly ONE process definition.
 * The app slug is used as the process slug — no separate process slug in the URL.
 */
@RestController
@RequestMapping("/adaptive/domains/{slug}/apps/{appSlug}/processes")
public class ProcessDefinitionController {

    @Autowired private OrganisationLookupPort organisationLookupPort;
    @Autowired private ApplicationLookupPort applicationLookupPort;
    @Autowired private PermissionService permissionService;
    @Autowired private ProcessDefinitionService definitionService;
    @Autowired private ProcessValidationService validationService;

    /** GET /processes/{processSlug} — get a specific process definition (latest version) */
    @GetMapping("/{processSlug}")
    public ResponseEntity<?> getProcess(@PathVariable String slug, @PathVariable String appSlug, @PathVariable String processSlug) {
        Context ctx = resolve(slug, appSlug);
        requireAppPermission(ctx.app().getId(), AppPermission.APP_VIEW_PROCESSES);

        ProcessDefinition def = definitionService.getProcess(ctx.domain().getId(), ctx.app().getId(), processSlug);
        ValidationResult valid = validationService.validate(def);
        return ResponseEntity.ok(ProcessDefinitionResponse.of(def, valid.isValid()));
    }

    /** GET /processes — get ALL latest processes for this app */
    @GetMapping
    public ResponseEntity<?> getAllProcesses(@PathVariable String slug, @PathVariable String appSlug) {
        Context ctx = resolve(slug, appSlug);
        requireAppPermission(ctx.app().getId(), AppPermission.APP_VIEW_PROCESSES);

        java.util.List<ProcessDefinition> defs = definitionService.getAllLatestProcesses(ctx.domain().getId(), ctx.app().getId());
        return ResponseEntity.ok(defs.stream().map(d -> ProcessDefinitionResponse.of(d, validationService.validate(d).isValid())).toList());
    }

    /** POST /processes/{processSlug} — create a process */
    @PostMapping("/{processSlug}")
    public ResponseEntity<?> createProcess(@PathVariable String slug, @PathVariable String appSlug, @PathVariable String processSlug,
            @Valid @RequestBody CreateProcessRequest request) {
        Context ctx = resolve(slug, appSlug);
        requireAppPermission(ctx.app().getId(), AppPermission.APP_MANAGE_PROCESSES);

        String userId = currentUserId();
        ProcessDefinition created = definitionService.createProcess(
                ctx.domain().getId(), ctx.app().getId(), processSlug, request, userId);
        ValidationResult valid = validationService.validate(created);
        return ResponseEntity.ok(ProcessDefinitionResponse.of(created, valid.isValid()));
    }

    /** PUT /processes/{processSlug} — update the DRAFT process */
    @PutMapping("/{processSlug}")
    public ResponseEntity<?> updateProcess(@PathVariable String slug, @PathVariable String appSlug, @PathVariable String processSlug,
            @RequestBody UpdateProcessRequest request) {
        Context ctx = resolve(slug, appSlug);
        requireAppPermission(ctx.app().getId(), AppPermission.APP_MANAGE_PROCESSES);

        ProcessDefinition updated = definitionService.updateProcess(
                ctx.domain().getId(), ctx.app().getId(), processSlug, request);
        ValidationResult valid = validationService.validate(updated);
        return ResponseEntity.ok(ProcessDefinitionResponse.of(updated, valid.isValid()));
    }

    /** DELETE /processes/{processSlug} — delete the DRAFT process */
    @DeleteMapping("/{processSlug}")
    public ResponseEntity<?> deleteProcess(@PathVariable String slug, @PathVariable String appSlug, @PathVariable String processSlug) {
        Context ctx = resolve(slug, appSlug);
        requireAppPermission(ctx.app().getId(), AppPermission.APP_MANAGE_PROCESSES);

        definitionService.deleteProcess(ctx.domain().getId(), ctx.app().getId(), processSlug);
        return ResponseEntity.noContent().build();
    }

    /** POST /processes/{processSlug}/publish — validate and publish the DRAFT */
    @PostMapping("/{processSlug}/publish")
    public ResponseEntity<?> publishProcess(@PathVariable String slug, @PathVariable String appSlug, @PathVariable String processSlug) {
        Context ctx = resolve(slug, appSlug);
        requireAppPermission(ctx.app().getId(), AppPermission.APP_MANAGE_PROCESSES);

        ProcessDefinition published = definitionService.publishProcess(
                ctx.domain().getId(), ctx.app().getId(), processSlug);
        return ResponseEntity.ok(ProcessDefinitionResponse.of(published, true));
    }

    /** POST /processes/{processSlug}/archive — archive the PUBLISHED process */
    @PostMapping("/{processSlug}/archive")
    public ResponseEntity<?> archiveProcess(@PathVariable String slug, @PathVariable String appSlug, @PathVariable String processSlug) {
        Context ctx = resolve(slug, appSlug);
        requireAppPermission(ctx.app().getId(), AppPermission.APP_MANAGE_PROCESSES);

        ProcessDefinition archived = definitionService.archiveProcess(
                ctx.domain().getId(), ctx.app().getId(), processSlug);
        return ResponseEntity.ok(archived);
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
}
