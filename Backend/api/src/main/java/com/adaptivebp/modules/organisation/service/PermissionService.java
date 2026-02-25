package com.adaptivebp.modules.organisation.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.adaptivebp.modules.appmanagement.model.AppGroup;
import com.adaptivebp.modules.appmanagement.model.AppGroupMember;
import com.adaptivebp.modules.appmanagement.permission.AppPermission;
import com.adaptivebp.modules.appmanagement.repository.AppGroupMemberRepository;
import com.adaptivebp.modules.appmanagement.repository.AppGroupRepository;
import com.adaptivebp.modules.organisation.model.DomainGroup;
import com.adaptivebp.modules.organisation.model.DomainGroupMember;
import com.adaptivebp.modules.organisation.permission.DomainPermission;
import com.adaptivebp.modules.organisation.repository.DomainGroupMemberRepository;
import com.adaptivebp.modules.organisation.repository.DomainGroupRepository;
import com.adaptivebp.shared.security.AdaptiveUserDetails;
import com.adaptivebp.shared.security.PrincipalType;

/**
 * ★ Organisation Module Facade for permissions — the public API for security checks.
 * Referenced by @PreAuthorize annotations as "permissionService".
 */
@Component("permissionService")
public class PermissionService {

    @Autowired
    private DomainGroupMemberRepository domainGroupMemberRepository;

    @Autowired
    private DomainGroupRepository domainGroupRepository;

    @Autowired
    private AppGroupRepository appGroupRepository;

    @Autowired
    private AppGroupMemberRepository appGroupMemberRepository;

    public boolean isOwner() {
        AdaptiveUserDetails principal = currentPrincipal();
        return principal != null && principal.getPrincipalType() == PrincipalType.OWNER;
    }

    public boolean hasDomainPermission(String domainId, DomainPermission permission) {
        AdaptiveUserDetails principal = currentPrincipal();
        return getDomainPermissions(domainId, principal).contains(permission);
    }

    public Set<DomainPermission> getDomainPermissions(String domainId) {
        return getDomainPermissions(domainId, currentPrincipal());
    }

    public Set<DomainPermission> getDomainPermissions(String domainId, AdaptiveUserDetails principal) {
        Set<DomainPermission> perms = EnumSet.noneOf(DomainPermission.class);
        if (principal == null) {
            return perms;
        }
        if (principal.getPrincipalType() == PrincipalType.OWNER) {
            perms.addAll(EnumSet.allOf(DomainPermission.class));
            return perms;
        }
        if (principal.getPrincipalType() == PrincipalType.DOMAIN_USER) {
            if (domainId == null || !domainId.equals(principal.getDomainId())) {
                return perms;
            }
            perms.add(DomainPermission.DOMAIN_USE_APP);
            List<DomainGroupMember> memberships = domainGroupMemberRepository
                    .findByDomainIdAndUserId(domainId, principal.getId());
            if (memberships.isEmpty()) {
                return perms;
            }
            Set<String> groupIds = memberships.stream().map(DomainGroupMember::getDomainGroupId).collect(Collectors.toSet());
            if (groupIds.isEmpty()) {
                return perms;
            }
            List<DomainGroup> groups = domainGroupRepository.findAllById(groupIds);
            for (DomainGroup group : groups) {
                perms.addAll(group.getPermissions());
            }
        }
        return perms;
    }

    public boolean hasAppPermission(String appId, AppPermission permission) {
        AdaptiveUserDetails principal = currentPrincipal();
        if (principal == null) {
            return false;
        }
        if (principal.getPrincipalType() == PrincipalType.OWNER) {
            return true;
        }
        if (principal.getPrincipalType() == PrincipalType.DOMAIN_USER) {
            return appGroupMemberRepository.findByAppIdAndUserId(appId, principal.getId()).stream()
                    .map(AppGroupMember::getGroupId)
                    .map(appGroupRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(AppGroup::getPermissions)
                    .flatMap(Set::stream)
                    .anyMatch(p -> p == permission);
        }
        return false;
    }

    private AdaptiveUserDetails currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AdaptiveUserDetails) {
            return (AdaptiveUserDetails) principal;
        }
        return null;
    }
}
