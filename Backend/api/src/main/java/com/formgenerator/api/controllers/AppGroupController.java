package com.formgenerator.api.controllers;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.formgenerator.api.dto.rbac.AssignMemberRequest;
import com.formgenerator.api.dto.rbac.CreateAppGroupRequest;
import com.formgenerator.api.models.app.AppGroup;
import com.formgenerator.api.models.app.AppGroupMember;
import com.formgenerator.api.models.app.Application;
import com.formgenerator.api.permissions.AppPermission;
import com.formgenerator.api.permissions.DomainPermission;
import com.formgenerator.api.repository.AppGroupMemberRepository;
import com.formgenerator.api.repository.AppGroupRepository;
import com.formgenerator.api.repository.ApplicationRepository;
import com.formgenerator.api.repository.DomainRepository;
import com.formgenerator.api.repository.DomainUserRepository;
import com.formgenerator.api.services.PermissionService;
import com.formgenerator.platform.auth.Domain;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/adaptive/domains/{slug}/apps/{appSlug}/groups")
public class AppGroupController {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private AppGroupRepository appGroupRepository;

    @Autowired
    private AppGroupMemberRepository appGroupMemberRepository;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private DomainUserRepository domainUserRepository;

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String slug, @PathVariable String appSlug) {
        ApplicationWithDomain appWithDomain = requireApplication(slug, appSlug);
        if (!permissionService.hasDomainPermission(appWithDomain.domain.getId(), DomainPermission.DOMAIN_MANAGE_APPS)) {
            return ResponseEntity.status(403).build();
        }
        List<AppGroup> groups = appGroupRepository.findByAppId(appWithDomain.application.getId());
        return ResponseEntity.ok(groups);
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable String slug, @PathVariable String appSlug,
            @Valid @RequestBody CreateAppGroupRequest request) {
        ApplicationWithDomain appWithDomain = requireApplication(slug, appSlug);
        if (!permissionService.hasDomainPermission(appWithDomain.domain.getId(), DomainPermission.DOMAIN_MANAGE_APPS)) {
            return ResponseEntity.status(403).build();
        }
        boolean exists = appGroupRepository.findByAppIdAndName(appWithDomain.application.getId(), request.getName())
                .isPresent();
        if (exists) {
            return ResponseEntity.badRequest().body("Group already exists");
        }
        AppGroup group = new AppGroup();
        group.setAppId(appWithDomain.application.getId());
        group.setName(request.getName());
        group.setPermissions(request.getPermissions());
        group.setDefaultGroup(false);
        return ResponseEntity.ok(appGroupRepository.save(group));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addMember(@PathVariable String slug, @PathVariable String appSlug,
            @PathVariable String groupId, @Valid @RequestBody AssignMemberRequest request) {
        ApplicationWithDomain appWithDomain = requireApplication(slug, appSlug);
        if (!permissionService.hasDomainPermission(appWithDomain.domain.getId(), DomainPermission.DOMAIN_MANAGE_APPS)) {
            return ResponseEntity.status(403).build();
        }
        AppGroup group = appGroupRepository.findById(groupId).orElse(null);
        if (group == null || !group.getAppId().equals(appWithDomain.application.getId())) {
            return ResponseEntity.notFound().build();
        }
        var userOpt = domainUserRepository.findByDomainIdAndUsername(appWithDomain.domain.getId(), request.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found in domain");
        }
        String userId = userOpt.get().getId();
        boolean exists = appGroupMemberRepository.findByGroupIdAndUserId(groupId, userId).isPresent();
        if (exists) {
            return ResponseEntity.badRequest().body("User already in group");
        }
        AppGroupMember member = new AppGroupMember();
        member.setGroupId(groupId);
        member.setAppId(appWithDomain.application.getId());
        member.setUserId(userId);
        member.setAssignedBy(currentPrincipalId());
        appGroupMemberRepository.save(member);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable String slug, @PathVariable String appSlug,
            @PathVariable String groupId, @PathVariable String userId) {
        ApplicationWithDomain appWithDomain = requireApplication(slug, appSlug);
        if (!permissionService.hasDomainPermission(appWithDomain.domain.getId(), DomainPermission.DOMAIN_MANAGE_APPS)) {
            return ResponseEntity.status(403).build();
        }
        AppGroup group = appGroupRepository.findById(groupId).orElse(null);
        if (group == null || !group.getAppId().equals(appWithDomain.application.getId())) {
            return ResponseEntity.notFound().build();
        }
        appGroupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .ifPresent(appGroupMemberRepository::delete);
        return ResponseEntity.ok().build();
    }

    private ApplicationWithDomain requireApplication(String slug, String appSlug) {
        Domain domain = domainRepository.findBySlug(slugify(slug))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found"));
        var application = applicationRepository.findByDomainIdAndSlug(domain.getId(), appSlug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        return new ApplicationWithDomain(domain, application);
    }

    private record ApplicationWithDomain(Domain domain, Application application) {
    }

    private String slugify(String input) {
        if (input == null)
            return null;
        String s = input.trim().toLowerCase();
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+", "").replaceAll("-+$", "");
        return s;
    }

    private String currentPrincipalId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof com.formgenerator.platform.auth.AdaptiveUserDetails details) {
            return details.getId();
        }
        return null;
    }
}
