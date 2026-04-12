package com.adaptivebp.modules.process.controller;

import java.util.List;
import java.util.Map;

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
import com.adaptivebp.modules.process.model.ProcessTemplate;
import com.adaptivebp.modules.process.repository.ProcessTemplateRepository;
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
@RequestMapping("/adaptive/domains/{slug}/apps/{appSlug}/process")
public class ProcessDefinitionController {

    @Autowired private OrganisationLookupPort organisationLookupPort;
    @Autowired private ApplicationLookupPort applicationLookupPort;
    @Autowired private PermissionService permissionService;
    @Autowired private ProcessDefinitionService definitionService;
    @Autowired private ProcessValidationService validationService;
    @Autowired private ProcessTemplateRepository templateRepository;

    /** GET /process — get this app's process definition (latest version) */
    @GetMapping
    public ResponseEntity<?> getProcess(@PathVariable String slug, @PathVariable String appSlug) {
        Context ctx = resolve(slug, appSlug);
        requireAppPermission(ctx.app().getId(), AppPermission.APP_VIEW);

        ProcessDefinition def = definitionService.getProcess(ctx.domain().getId(), ctx.app().getId(), appSlug);
        ValidationResult valid = validationService.validate(def);
        return ResponseEntity.ok(ProcessDefinitionResponse.of(def, valid.isValid()));
    }

    /** POST /process — create the app's process (only allowed once; app slug used as process slug) */
    @PostMapping
    public ResponseEntity<?> createProcess(@PathVariable String slug, @PathVariable String appSlug,
            @Valid @RequestBody CreateProcessRequest request) {
        Context ctx = resolve(slug, appSlug);
        requireAppPermission(ctx.app().getId(), AppPermission.APP_MANAGE_WORKFLOW);

        String userId = currentUserId();
        ProcessDefinition created = definitionService.createProcess(
                ctx.domain().getId(), ctx.app().getId(), appSlug, request, userId);
        ValidationResult valid = validationService.validate(created);
        return ResponseEntity.ok(ProcessDefinitionResponse.of(created, valid.isValid()));
    }

    /** PUT /process — update the DRAFT process */
    @PutMapping
    public ResponseEntity<?> updateProcess(@PathVariable String slug, @PathVariable String appSlug,
            @RequestBody UpdateProcessRequest request) {
        Context ctx = resolve(slug, appSlug);
        requireAppPermission(ctx.app().getId(), AppPermission.APP_MANAGE_WORKFLOW);

        ProcessDefinition updated = definitionService.updateProcess(
                ctx.domain().getId(), ctx.app().getId(), appSlug, request);
        ValidationResult valid = validationService.validate(updated);
        return ResponseEntity.ok(ProcessDefinitionResponse.of(updated, valid.isValid()));
    }

    /** DELETE /process — delete the DRAFT process */
    @DeleteMapping
    public ResponseEntity<?> deleteProcess(@PathVariable String slug, @PathVariable String appSlug) {
        Context ctx = resolve(slug, appSlug);
        requireAppPermission(ctx.app().getId(), AppPermission.APP_MANAGE_WORKFLOW);

        definitionService.deleteProcess(ctx.domain().getId(), ctx.app().getId(), appSlug);
        return ResponseEntity.noContent().build();
    }

    /** POST /process/publish — validate and publish the DRAFT */
    @PostMapping("/publish")
    public ResponseEntity<?> publishProcess(@PathVariable String slug, @PathVariable String appSlug) {
        Context ctx = resolve(slug, appSlug);
        requireAppPermission(ctx.app().getId(), AppPermission.APP_MANAGE_WORKFLOW);

        ProcessDefinition published = definitionService.publishProcess(
                ctx.domain().getId(), ctx.app().getId(), appSlug);
        return ResponseEntity.ok(ProcessDefinitionResponse.of(published, true));
    }

    /** POST /process/archive — archive the PUBLISHED process */
    @PostMapping("/archive")
    public ResponseEntity<?> archiveProcess(@PathVariable String slug, @PathVariable String appSlug) {
        Context ctx = resolve(slug, appSlug);
        requireAppPermission(ctx.app().getId(), AppPermission.APP_MANAGE_WORKFLOW);

        ProcessDefinition archived = definitionService.archiveProcess(
                ctx.domain().getId(), ctx.app().getId(), appSlug);
        return ResponseEntity.ok(archived);
    }

    // ── Process Templates ─────────────────────────────────────────────────────

    /** GET /process/templates — list all available process templates */
    @GetMapping("/templates")
    public ResponseEntity<?> listTemplates(@PathVariable String slug, @PathVariable String appSlug) {
        Context ctx = resolve(slug, appSlug);
        requireAppPermission(ctx.app().getId(), AppPermission.APP_MANAGE_WORKFLOW);

        List<ProcessTemplate> templates = templateRepository.findAll();
        return ResponseEntity.ok(templates);
    }

    /** POST /process/from-template — create process from template */
    @PostMapping("/from-template")
    public ResponseEntity<?> createFromTemplate(@PathVariable String slug, @PathVariable String appSlug,
            @RequestBody Map<String, Object> body) {
        Context ctx = resolve(slug, appSlug);
        requireAppPermission(ctx.app().getId(), AppPermission.APP_MANAGE_WORKFLOW);

        String templateId = (String) body.get("templateId");
        if (templateId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "templateId is required");
        }

        // Extract linkedModelIds if provided
        @SuppressWarnings("unchecked")
        List<String> linkedModelIds = (List<String>) body.get("linkedModelIds");

        ProcessTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));

        // Create a new process definition from the template
        ProcessDefinition process = definitionService.createFromTemplate(
                ctx.domain().getId(),
                ctx.app().getId(),
                appSlug,
                template,
                linkedModelIds,
                currentUserId()
        );

        return ResponseEntity.ok(ProcessDefinitionResponse.of(process, false));
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
