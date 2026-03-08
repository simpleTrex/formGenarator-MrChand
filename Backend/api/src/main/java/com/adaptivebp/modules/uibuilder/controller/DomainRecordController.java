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
import com.adaptivebp.modules.formbuilder.model.DomainModel;
import com.adaptivebp.modules.formbuilder.repository.DomainModelRepository;
import com.adaptivebp.modules.organisation.model.Organisation;
import com.adaptivebp.modules.organisation.port.OrganisationLookupPort;
import com.adaptivebp.modules.organisation.service.PermissionService;
import com.adaptivebp.modules.uibuilder.dto.CreateRecordRequest;
import com.adaptivebp.modules.uibuilder.model.DomainRecord;
import com.adaptivebp.modules.uibuilder.repository.DomainRecordRepository;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/adaptive/domains/{slug}/models/{modelSlug}/records")
public class DomainRecordController {

    @Autowired
    private OrganisationLookupPort organisationLookupPort;
    @Autowired
    private ApplicationLookupPort applicationLookupPort;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private DomainModelRepository domainModelRepository;
    @Autowired
    private DomainRecordRepository recordRepository;

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String slug, @PathVariable String modelSlug,
            @RequestParam(name = "appSlug") String appSlug) {
        Organisation domain = requireDomain(slug);
        Application app = requireApplication(domain.getId(), appSlug);
        if (!permissionService.hasAppPermission(app.getId(), AppPermission.APP_READ)) {
            return ResponseEntity.status(403).build();
        }
        DomainModel model = requireModel(domain.getId(), modelSlug);
        List<DomainRecord> records = recordRepository.findByModelIdAndDomainSlug(model.getId(), slug);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<?> getOne(@PathVariable String slug, @PathVariable String modelSlug,
            @PathVariable String recordId, @RequestParam(name = "appSlug") String appSlug) {
        Organisation domain = requireDomain(slug);
        Application app = requireApplication(domain.getId(), appSlug);
        if (!permissionService.hasAppPermission(app.getId(), AppPermission.APP_READ)) {
            return ResponseEntity.status(403).build();
        }
        DomainRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found"));
        return ResponseEntity.ok(record);
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable String slug, @PathVariable String modelSlug,
            @RequestParam(name = "appSlug") String appSlug,
            @RequestBody CreateRecordRequest request,
            HttpServletRequest httpRequest) {
        Organisation domain = requireDomain(slug);
        Application app = requireApplication(domain.getId(), appSlug);
        if (!permissionService.hasAppPermission(app.getId(), AppPermission.APP_WRITE)) {
            return ResponseEntity.status(403).build();
        }
        DomainModel model = requireModel(domain.getId(), modelSlug);
        DomainRecord record = new DomainRecord();
        record.setModelId(model.getId());
        record.setModelSlug(model.getSlug());
        record.setAppId(app.getId());
        record.setDomainSlug(slug);
        record.setData(request.getData());
        return ResponseEntity.ok(recordRepository.save(record));
    }

    @PutMapping("/{recordId}")
    public ResponseEntity<?> update(@PathVariable String slug, @PathVariable String modelSlug,
            @PathVariable String recordId,
            @RequestParam(name = "appSlug") String appSlug,
            @RequestBody CreateRecordRequest request) {
        Organisation domain = requireDomain(slug);
        Application app = requireApplication(domain.getId(), appSlug);
        if (!permissionService.hasAppPermission(app.getId(), AppPermission.APP_WRITE)) {
            return ResponseEntity.status(403).build();
        }
        DomainRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found"));
        if (request.getData() != null)
            record.setData(request.getData());
        return ResponseEntity.ok(recordRepository.save(record));
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<?> delete(@PathVariable String slug, @PathVariable String modelSlug,
            @PathVariable String recordId, @RequestParam(name = "appSlug") String appSlug) {
        Organisation domain = requireDomain(slug);
        Application app = requireApplication(domain.getId(), appSlug);
        if (!permissionService.hasAppPermission(app.getId(), AppPermission.APP_WRITE)) {
            return ResponseEntity.status(403).build();
        }
        recordRepository.deleteById(recordId);
        return ResponseEntity.noContent().build();
    }

    private DomainModel requireModel(String domainId, String modelSlug) {
        return domainModelRepository.findByDomainIdAndSlug(domainId, slugify(modelSlug))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Model not found"));
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
