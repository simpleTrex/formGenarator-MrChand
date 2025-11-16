package com.formgenerator.api.controllers;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.formgenerator.api.models.domain.DomainGroup;
import com.formgenerator.api.models.domain.DomainGroupMember;
import com.formgenerator.api.permissions.DomainPermission;
import com.formgenerator.api.repository.DomainGroupMemberRepository;
import com.formgenerator.api.repository.DomainGroupRepository;
import com.formgenerator.api.repository.DomainRepository;
import com.formgenerator.api.repository.DomainUserRepository;
import com.formgenerator.api.responses.DomainAccessResponse;
import com.formgenerator.api.services.PermissionService;
import com.formgenerator.platform.auth.AdaptiveUserDetails;
import com.formgenerator.platform.auth.Domain;
import com.formgenerator.platform.auth.PrincipalType;

@RestController
@RequestMapping("/adaptive/domains/{slug}/access")
public class DomainAccessController {

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private DomainGroupMemberRepository domainGroupMemberRepository;

    @Autowired
    private DomainGroupRepository domainGroupRepository;

    @Autowired
    private PermissionService permissionService;

    @GetMapping("/me")
    public ResponseEntity<?> myAccess(@PathVariable String slug) {
        Domain domain = requireDomain(slug);
        AdaptiveUserDetails principal = currentPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Set<DomainPermission> permissions = permissionService.getDomainPermissions(domain.getId(), principal);
        List<String> groups = resolveGroupsForPrincipal(domain.getId(), principal);
        return ResponseEntity.ok(new DomainAccessResponse(permissions, groups, principal.getPrincipalType()));
    }

    private List<String> resolveGroupsForPrincipal(String domainId, AdaptiveUserDetails principal) {
        if (principal.getPrincipalType() == PrincipalType.OWNER) {
            return List.of("Domain Admin");
        }
        if (principal.getPrincipalType() == PrincipalType.DOMAIN_USER) {
            List<DomainGroupMember> memberships = domainGroupMemberRepository.findByDomainIdAndUserId(domainId,
                    principal.getId());
            if (memberships.isEmpty()) {
                return Collections.emptyList();
            }
            Set<String> groupIds = memberships.stream()
                    .map(DomainGroupMember::getDomainGroupId)
                    .collect(Collectors.toSet());
            return domainGroupRepository.findAllById(groupIds).stream()
                    .map(DomainGroup::getName)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private Domain requireDomain(String slug) {
        return domainRepository.findBySlug(slugify(slug))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found"));
    }

    private AdaptiveUserDetails currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AdaptiveUserDetails details) {
            return details;
        }
        return null;
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
