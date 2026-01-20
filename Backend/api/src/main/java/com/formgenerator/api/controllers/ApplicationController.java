package com.formgenerator.api.controllers;

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

import com.formgenerator.api.dto.app.CreateApplicationRequest;
import com.formgenerator.api.models.app.Application;
import com.formgenerator.api.permissions.DomainPermission;
import com.formgenerator.api.repository.ApplicationRepository;
import com.formgenerator.api.repository.DomainRepository;
import com.formgenerator.api.services.ApplicationProvisioningService;
import com.formgenerator.api.services.PermissionService;
import com.formgenerator.platform.auth.Domain;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/adaptive/domains/{slug}/apps")
public class ApplicationController {

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ApplicationProvisioningService applicationProvisioningService;

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String slug) {
        Domain domain = requireDomain(slug);
        if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_USE_APP)) {
            return ResponseEntity.status(403).build();
        }
        List<Application> apps = applicationRepository.findByDomainId(domain.getId());
        return ResponseEntity.ok(apps);
    }

    @GetMapping("/{appSlug}")
    public ResponseEntity<?> getApplication(@PathVariable String slug, @PathVariable String appSlug) {
        Domain domain = requireDomain(slug);
        if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_USE_APP)) {
            return ResponseEntity.status(403).build();
        }
        Application app = applicationRepository.findByDomainIdAndSlug(domain.getId(), appSlug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        return ResponseEntity.ok(app);
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable String slug, @Valid @RequestBody CreateApplicationRequest request) {
        Domain domain = requireDomain(slug);
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

    private Domain requireDomain(String slug) {
        return domainRepository.findBySlug(slugify(slug))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found"));
    }

    private String slugify(String input) {
        if (input == null)
            return null;
        String s = input.trim().toLowerCase();
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+", "").replaceAll("-+$", "");
        return s;
    }
}
