package com.formgenerator.api.controllers;

import java.util.List;
import java.util.stream.Collectors;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.formgenerator.api.dto.model.CreateDomainModelRequest;
import com.formgenerator.api.dto.model.UpdateDomainModelRequest;
import com.formgenerator.api.models.app.Application;
import com.formgenerator.api.models.domain.model.DomainModel;
import com.formgenerator.api.permissions.AppPermission;
import com.formgenerator.api.repository.ApplicationRepository;
import com.formgenerator.api.repository.DomainModelRepository;
import com.formgenerator.api.repository.DomainRepository;
import com.formgenerator.api.services.PermissionService;
import com.formgenerator.platform.auth.Domain;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/adaptive/domains/{slug}/models")
public class DomainModelController {

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private DomainModelRepository domainModelRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private PermissionService permissionService;

    @GetMapping
    public ResponseEntity<?> list(
            @PathVariable String slug,
            @RequestParam(name = "appSlug") String appSlug) {
        Domain domain = requireDomain(slug);
        Application app = requireApplication(domain.getId(), appSlug);
        if (!permissionService.hasAppPermission(app.getId(), AppPermission.APP_WRITE)) {
            return ResponseEntity.status(403).build();
        }

        List<DomainModel> models = domainModelRepository.findByDomainId(domain.getId());
        return ResponseEntity.ok(models);
    }

    @GetMapping("/{modelSlug}")
    public ResponseEntity<?> get(
            @PathVariable String slug,
            @PathVariable String modelSlug,
            @RequestParam(name = "appSlug") String appSlug) {
        Domain domain = requireDomain(slug);
        Application app = requireApplication(domain.getId(), appSlug);
        if (!permissionService.hasAppPermission(app.getId(), AppPermission.APP_WRITE)) {
            return ResponseEntity.status(403).build();
        }

        DomainModel model = domainModelRepository.findByDomainIdAndSlug(domain.getId(), slugify(modelSlug))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Model not found"));
        return ResponseEntity.ok(model);
    }

    @PostMapping
    public ResponseEntity<?> create(
            @PathVariable String slug,
            @RequestParam(name = "appSlug") String appSlug,
            @Valid @RequestBody CreateDomainModelRequest request) {
        Domain domain = requireDomain(slug);
        Application app = requireApplication(domain.getId(), appSlug);
        if (!permissionService.hasAppPermission(app.getId(), AppPermission.APP_WRITE)) {
            return ResponseEntity.status(403).build();
        }

        String normalizedSlug = slugify(request.getSlug());
        if (domainModelRepository.existsByDomainIdAndSlug(domain.getId(), normalizedSlug)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Model slug already exists");
        }

        DomainModel model = new DomainModel();
        model.setDomainId(domain.getId());
        model.setSlug(normalizedSlug);
        model.setName(request.getName());
        model.setDescription(request.getDescription());
        model.setSharedWithAllApps(request.isSharedWithAllApps());
        if (request.getAllowedAppIds() != null) {
            model.getAllowedAppIds().clear();
            model.getAllowedAppIds().addAll(request.getAllowedAppIds());
        }
        if (request.getFields() != null) {
            model.setFields(request.getFields());
        }

        DomainModel saved = domainModelRepository.save(model);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{modelSlug}")
    public ResponseEntity<?> update(
            @PathVariable String slug,
            @PathVariable String modelSlug,
            @RequestParam(name = "appSlug") String appSlug,
            @Valid @RequestBody UpdateDomainModelRequest request) {
        Domain domain = requireDomain(slug);
        Application app = requireApplication(domain.getId(), appSlug);
        if (!permissionService.hasAppPermission(app.getId(), AppPermission.APP_WRITE)) {
            return ResponseEntity.status(403).build();
        }

        DomainModel model = domainModelRepository.findByDomainIdAndSlug(domain.getId(), slugify(modelSlug))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Model not found"));

        if (request.getName() != null) {
            model.setName(request.getName());
        }
        if (request.getDescription() != null) {
            model.setDescription(request.getDescription());
        }
        if (request.getSharedWithAllApps() != null) {
            model.setSharedWithAllApps(request.getSharedWithAllApps());
        }
        if (request.getAllowedAppIds() != null) {
            model.getAllowedAppIds().clear();
            model.getAllowedAppIds().addAll(request.getAllowedAppIds());
        }
        if (request.getFields() != null) {
            model.setFields(request.getFields());
            model.setVersion(model.getVersion() + 1);
        }

        DomainModel saved = domainModelRepository.save(model);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{modelSlug}")
    public ResponseEntity<?> delete(
            @PathVariable String slug,
            @PathVariable String modelSlug,
            @RequestParam(name = "appSlug") String appSlug) {
        Domain domain = requireDomain(slug);
        Application app = requireApplication(domain.getId(), appSlug);
        if (!permissionService.hasAppPermission(app.getId(), AppPermission.APP_WRITE)) {
            return ResponseEntity.status(403).build();
        }

        DomainModel model = domainModelRepository.findByDomainIdAndSlug(domain.getId(), slugify(modelSlug))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Model not found"));

        domainModelRepository.delete(model);
        return ResponseEntity.noContent().build();
    }

    private Application requireApplication(String domainId, String appSlug) {
        if (appSlug == null || appSlug.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "appSlug is required");
        }
        return applicationRepository.findByDomainIdAndSlug(domainId, slugify(appSlug))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    private Domain requireDomain(String slug) {
        return domainRepository.findBySlug(slugify(slug))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found"));
    }

    private String slugify(String input) {
        if (input == null) {
            return null;
        }
        String s = input.trim().toLowerCase();
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+", "").replaceAll("-+$", "");
        return s;
    }
}
