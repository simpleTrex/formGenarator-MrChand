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
import com.formgenerator.api.dto.rbac.CreateDomainGroupRequest;
import com.formgenerator.api.models.domain.DomainGroup;
import com.formgenerator.api.models.domain.DomainGroupMember;
import com.formgenerator.api.permissions.DomainPermission;
import com.formgenerator.api.repository.DomainGroupMemberRepository;
import com.formgenerator.api.repository.DomainGroupRepository;
import com.formgenerator.api.repository.DomainRepository;
import com.formgenerator.api.repository.DomainUserRepository;
import com.formgenerator.api.services.PermissionService;
import com.formgenerator.platform.auth.Domain;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/adaptive/domains/{slug}/groups")
public class DomainGroupController {

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private DomainGroupRepository domainGroupRepository;

    @Autowired
    private DomainGroupMemberRepository domainGroupMemberRepository;

    @Autowired
    private DomainUserRepository domainUserRepository;

    @Autowired
    private PermissionService permissionService;

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String slug) {
        Domain domain = requireDomain(slug);
        if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_MANAGE_USERS)) {
            return ResponseEntity.status(403).build();
        }
        List<DomainGroup> groups = domainGroupRepository.findByDomainId(domain.getId());
        return ResponseEntity.ok(groups);
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable String slug, @Valid @RequestBody CreateDomainGroupRequest request) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Domain groups are managed automatically and cannot be created manually.");
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addMember(@PathVariable String slug, @PathVariable String groupId,
            @Valid @RequestBody AssignMemberRequest request) {
        Domain domain = requireDomain(slug);
        DomainGroup group = domainGroupRepository.findById(groupId).orElse(null);
        if (group == null || !domain.getId().equals(group.getDomainId())) {
            return ResponseEntity.notFound().build();
        }
        if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_MANAGE_USERS)) {
            return ResponseEntity.status(403).build();
        }
        var userOpt = domainUserRepository.findByDomainIdAndUsername(domain.getId(), request.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found in this domain");
        }
        String userId = userOpt.get().getId();
        boolean exists = domainGroupMemberRepository.findByDomainGroupIdAndUserId(groupId, userId)
                .isPresent();
        if (exists) {
            return ResponseEntity.badRequest().body("User already in group");
        }
        DomainGroupMember member = new DomainGroupMember();
        member.setDomainGroupId(groupId);
        member.setDomainId(domain.getId());
        member.setUserId(userId);
        member.setAssignedBy(currentPrincipalId());
        domainGroupMemberRepository.save(member);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable String slug, @PathVariable String groupId,
            @PathVariable String userId) {
        Domain domain = requireDomain(slug);
        DomainGroup group = domainGroupRepository.findById(groupId).orElse(null);
        if (group == null || !domain.getId().equals(group.getDomainId())) {
            return ResponseEntity.notFound().build();
        }
        if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_MANAGE_USERS)) {
            return ResponseEntity.status(403).build();
        }
        domainGroupMemberRepository.findByDomainGroupIdAndUserId(groupId, userId)
                .ifPresent(domainGroupMemberRepository::delete);
        return ResponseEntity.ok().build();
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

    private String currentPrincipalId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof com.formgenerator.platform.auth.AdaptiveUserDetails details) {
            return details.getId();
        }
        return null;
    }
}
