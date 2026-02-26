package com.adaptivebp.modules.appmanagement.controller;

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

import com.adaptivebp.modules.appmanagement.dto.request.CreateApplicationRequest;
import com.adaptivebp.modules.appmanagement.model.Application;
import com.adaptivebp.modules.appmanagement.repository.ApplicationRepository;
import com.adaptivebp.modules.appmanagement.service.ApplicationProvisioningService;
import com.adaptivebp.modules.organisation.model.Organisation;
import com.adaptivebp.modules.organisation.permission.DomainPermission;
import com.adaptivebp.modules.organisation.port.OrganisationLookupPort;
import com.adaptivebp.modules.organisation.service.PermissionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/adaptive/domains/{slug}/apps")
public class ApplicationController {

    @Autowired
    private OrganisationLookupPort organisationLookupPort;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ApplicationProvisioningService applicationProvisioningService;

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String slug) {
        Organisation domain = requireDomain(slug);
        if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_USE_APP)) {
            return ResponseEntity.status(403).build();
        }
        List<Application> apps = applicationRepository.findByDomainId(domain.getId());
        return ResponseEntity.ok(apps);
    }

    @GetMapping("/{appSlug}")
    public ResponseEntity<?> getApplication(@PathVariable String slug, @PathVariable String appSlug) {
        Organisation domain = requireDomain(slug);
        if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_USE_APP)) {
            return ResponseEntity.status(403).build();
        }
        Application app = applicationRepository.findByDomainIdAndSlug(domain.getId(), appSlug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        return ResponseEntity.ok(app);
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable String slug, @Valid @RequestBody CreateApplicationRequest request) {
        Organisation domain = requireDomain(slug);
        if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_MANAGE_APPS)) {
            return ResponseEntity.status(403).build();
        }
        if (applicationRepository.existsByDomainIdAndSlug(domain.getId(), request.getSlug())) {
            return ResponseEntity.badRequest().body("Slug already exists");
        }
        Application application = new Application();
        application.setDomainId(domain.getId());
        application.setSlug(request.getSlug());
        application.setName(request.getName());
        application.setOwnerUserId(request.getOwnerUserId());
        Application saved = applicationRepository.save(application);
        applicationProvisioningService.provisionDefaultGroups(saved, request.getOwnerUserId());
        return ResponseEntity.ok(saved);
    }

    private Organisation requireDomain(String slug) {
        return organisationLookupPort.findBySlug(slugify(slug))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found"));
    }

    private String slugify(String input) {
        if (input == null) return null;
        String s = input.trim().toLowerCase();
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+", "").replaceAll("-+$", "");
        return s;
    }
}
