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
import com.adaptivebp.modules.uibuilder.dto.CreatePageRequest;
import com.adaptivebp.modules.uibuilder.dto.SaveLayoutRequest;
import com.adaptivebp.modules.uibuilder.model.AppPage;
import com.adaptivebp.modules.uibuilder.model.PageLayout;
import com.adaptivebp.modules.uibuilder.repository.AppPageRepository;
import com.adaptivebp.modules.uibuilder.repository.PageLayoutRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/adaptive/domains/{slug}/apps/{appSlug}/pages")
public class AppPageController {

    @Autowired
    private OrganisationLookupPort organisationLookupPort;
    @Autowired
    private ApplicationLookupPort applicationLookupPort;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private AppPageRepository pageRepository;
    @Autowired
    private PageLayoutRepository layoutRepository;

    // ───── Page CRUD ─────

    @GetMapping
    public ResponseEntity<?> listPages(@PathVariable String slug, @PathVariable String appSlug) {
        Organisation domain = requireDomain(slug);
        Application app = requireApplication(domain.getId(), appSlug);
        if (!permissionService.hasAppPermission(app.getId(), AppPermission.APP_READ)) {
            return ResponseEntity.status(403).build();
        }
        List<AppPage> pages = pageRepository.findByAppIdAndDomainSlug(app.getId(), slug);
        return ResponseEntity.ok(pages);
    }

    @PostMapping
    public ResponseEntity<?> createPage(@PathVariable String slug, @PathVariable String appSlug,
            @Valid @RequestBody CreatePageRequest request) {
        Organisation domain = requireDomain(slug);
        Application app = requireApplication(domain.getId(), appSlug);
        if (!permissionService.hasAppPermission(app.getId(), AppPermission.APP_WRITE)) {
            return ResponseEntity.status(403).build();
        }
        String pageSlug = slugify(request.getSlug() != null ? request.getSlug() : request.getName());
        if (pageRepository.existsByAppIdAndSlug(app.getId(), pageSlug)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Page slug already exists");
        }
        AppPage page = new AppPage();
        page.setAppId(app.getId());
        page.setAppSlug(appSlug);
        page.setDomainSlug(slug);
        page.setName(request.getName());
        page.setSlug(pageSlug);
        page.setOrder(request.getOrder());
        return ResponseEntity.ok(pageRepository.save(page));
    }

    @PutMapping("/{pageId}")
    public ResponseEntity<?> updatePage(@PathVariable String slug, @PathVariable String appSlug,
            @PathVariable String pageId, @RequestBody CreatePageRequest request) {
        Organisation domain = requireDomain(slug);
        Application app = requireApplication(domain.getId(), appSlug);
        if (!permissionService.hasAppPermission(app.getId(), AppPermission.APP_WRITE)) {
            return ResponseEntity.status(403).build();
        }
        AppPage page = pageRepository.findById(pageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found"));
        if (request.getName() != null)
            page.setName(request.getName());
        page.setOrder(request.getOrder());
        return ResponseEntity.ok(pageRepository.save(page));
    }

    @DeleteMapping("/{pageId}")
    public ResponseEntity<?> deletePage(@PathVariable String slug, @PathVariable String appSlug,
            @PathVariable String pageId) {
        Organisation domain = requireDomain(slug);
        Application app = requireApplication(domain.getId(), appSlug);
        if (!permissionService.hasAppPermission(app.getId(), AppPermission.APP_WRITE)) {
            return ResponseEntity.status(403).build();
        }
        pageRepository.deleteById(pageId);
        layoutRepository.findByPageIdAndAppId(pageId, app.getId())
                .ifPresent(layoutRepository::delete);
        return ResponseEntity.noContent().build();
    }

    // ───── Layout endpoints ─────

    @GetMapping("/{pageId}/layout")
    public ResponseEntity<?> getLayout(@PathVariable String slug, @PathVariable String appSlug,
            @PathVariable String pageId) {
        Organisation domain = requireDomain(slug);
        Application app = requireApplication(domain.getId(), appSlug);
        if (!permissionService.hasAppPermission(app.getId(), AppPermission.APP_READ)) {
            return ResponseEntity.status(403).build();
        }
        PageLayout layout = layoutRepository.findByPageIdAndAppId(pageId, app.getId())
                .orElseGet(() -> {
                    PageLayout empty = new PageLayout();
                    empty.setPageId(pageId);
                    empty.setAppId(app.getId());
                    return empty;
                });
        return ResponseEntity.ok(layout);
    }

    @PutMapping("/{pageId}/layout")
    public ResponseEntity<?> saveLayout(@PathVariable String slug, @PathVariable String appSlug,
            @PathVariable String pageId, @RequestBody SaveLayoutRequest request) {
        Organisation domain = requireDomain(slug);
        Application app = requireApplication(domain.getId(), appSlug);
        if (!permissionService.hasAppPermission(app.getId(), AppPermission.APP_WRITE)) {
            return ResponseEntity.status(403).build();
        }
        // Verify page belongs to this app
        pageRepository.findById(pageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found"));

        PageLayout layout = layoutRepository.findByPageIdAndAppId(pageId, app.getId())
                .orElseGet(() -> {
                    PageLayout l = new PageLayout();
                    l.setPageId(pageId);
                    l.setAppId(app.getId());
                    return l;
                });
        layout.setLayout(request.getLayout());
        return ResponseEntity.ok(layoutRepository.save(layout));
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
