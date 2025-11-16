package com.formgenerator.api.services;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.formgenerator.api.models.app.AppGroup;
import com.formgenerator.api.models.app.AppGroupMember;
import com.formgenerator.api.models.domain.DomainGroup;
import com.formgenerator.api.models.domain.DomainGroupMember;
import com.formgenerator.api.permissions.AppPermission;
import com.formgenerator.api.permissions.DomainPermission;
import com.formgenerator.api.repository.AppGroupMemberRepository;
import com.formgenerator.api.repository.AppGroupRepository;
import com.formgenerator.api.repository.DomainGroupMemberRepository;
import com.formgenerator.api.repository.DomainGroupRepository;
import com.formgenerator.platform.auth.AdaptiveUserDetails;
import com.formgenerator.platform.auth.PrincipalType;

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
                    .filter(java.util.Optional::isPresent)
                    .map(java.util.Optional::get)
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
