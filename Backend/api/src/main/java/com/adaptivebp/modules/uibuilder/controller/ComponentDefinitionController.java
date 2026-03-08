package com.adaptivebp.modules.uibuilder.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.adaptivebp.modules.appmanagement.model.Application;
import com.adaptivebp.modules.appmanagement.permission.AppPermission;
import com.adaptivebp.modules.appmanagement.port.ApplicationLookupPort;
import com.adaptivebp.modules.organisation.model.Organisation;
import com.adaptivebp.modules.organisation.port.OrganisationLookupPort;
import com.adaptivebp.modules.organisation.service.PermissionService;
import com.adaptivebp.modules.uibuilder.dto.CreateComponentRequest;
import com.adaptivebp.modules.uibuilder.model.ComponentDefinition;
import com.adaptivebp.modules.uibuilder.repository.ComponentDefinitionRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/adaptive/domains/{slug}/apps/{appSlug}/components")
public class ComponentDefinitionController {

    @Autowired
    private OrganisationLookupPort organisationLookupPort;
    @Autowired
    private ApplicationLookupPort applicationLookupPort;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private ComponentDefinitionRepository componentRepository;

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String slug, @PathVariable String appSlug) {
        Organisation domain = requireDomain(slug);
        Application app = requireApplication(domain.getId(), appSlug);
        if (!permissionService.hasAppPermission(app.getId(), AppPermission.APP_WRITE)) {
            return ResponseEntity.status(403).build();
        }
        List<ComponentDefinition> components = componentRepository.findByAppIdAndDomainSlug(app.getId(), slug);
        return ResponseEntity.ok(components);
    }

    @GetMapping("/{compId}")
    public ResponseEntity<?> getOne(@PathVariable String slug, @PathVariable String appSlug,
            @PathVariable String compId) {
        Organisation domain = requireDomain(slug);
        Application app = requireApplication(domain.getId(), appSlug);
        if (!permissionService.hasAppPermission(app.getId(), AppPermission.APP_READ)) {
            return ResponseEntity.status(403).build();
        }
        ComponentDefinition comp = componentRepository.findById(compId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Component not found"));
        return ResponseEntity.ok(comp);
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable String slug, @PathVariable String appSlug,
            @Valid @RequestBody CreateComponentRequest request) {
        Organisation domain = requireDomain(slug);
        Application app = requireApplication(domain.getId(), appSlug);
        if (!permissionService.hasAppPermission(app.getId(), AppPermission.APP_WRITE)) {
            return ResponseEntity.status(403).build();
        }
        ComponentDefinition comp = new ComponentDefinition();
        comp.setAppId(app.getId());
        comp.setAppSlug(appSlug);
        comp.setDomainSlug(slug);
        comp.setName(request.getName());
        comp.setPrimitiveType(request.getPrimitiveType());
        comp.setModelId(request.getModelId());
        comp.setConfig(request.getConfig());
        return ResponseEntity.ok(componentRepository.save(comp));
    }

    @PutMapping("/{compId}")
    public ResponseEntity<?> update(@PathVariable String slug, @PathVariable String appSlug,
            @PathVariable String compId, @RequestBody CreateComponentRequest request) {
        Organisation domain = requireDomain(slug);
        Application app = requireApplication(domain.getId(), appSlug);
        if (!permissionService.hasAppPermission(app.getId(), AppPermission.APP_WRITE)) {
            return ResponseEntity.status(403).build();
        }
        ComponentDefinition comp = componentRepository.findById(compId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Component not found"));
        if (request.getName() != null)
            comp.setName(request.getName());
        if (request.getPrimitiveType() != null)
            comp.setPrimitiveType(request.getPrimitiveType());
        if (request.getModelId() != null)
            comp.setModelId(request.getModelId());
        if (request.getConfig() != null)
            comp.setConfig(request.getConfig());
        return ResponseEntity.ok(componentRepository.save(comp));
    }

    @DeleteMapping("/{compId}")
    public ResponseEntity<?> delete(@PathVariable String slug, @PathVariable String appSlug,
            @PathVariable String compId) {
        Organisation domain = requireDomain(slug);
        Application app = requireApplication(domain.getId(), appSlug);
        if (!permissionService.hasAppPermission(app.getId(), AppPermission.APP_WRITE)) {
            return ResponseEntity.status(403).build();
        }
        componentRepository.deleteById(compId);
        return ResponseEntity.noContent().build();
    }

    private Organisation requireDomain(String slug) {
        return organisationLookupPort.findBySlug(slugify(slug))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found"));
    }

    private Application requireApplication(String domainId, String appSlug) {
        return applicationLookupPort.findByDomainIdAndSlug(domainId, slugify(appSlug))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    private String slugify(String input) {
        if (input == null)
            return null;
        return input.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+", "").replaceAll("-+$", "");
    }
}
