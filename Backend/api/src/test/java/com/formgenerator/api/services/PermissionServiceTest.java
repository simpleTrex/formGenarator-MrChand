package com.formgenerator.api.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private DomainGroupMemberRepository domainGroupMemberRepository;

    @Mock
    private DomainGroupRepository domainGroupRepository;

    @Mock
    private AppGroupRepository appGroupRepository;

    @Mock
    private AppGroupMemberRepository appGroupMemberRepository;

    @InjectMocks
    private PermissionService permissionService;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void hasDomainPermission_returnsTrueForOwner() {
        AdaptiveUserDetails owner = AdaptiveUserDetails.owner("owner-1", "owner@example.com", "hash");
        authenticate(owner);

        assertTrue(permissionService.hasDomainPermission("domain-1", DomainPermission.DOMAIN_MANAGE));
    }

    @Test
    void hasDomainPermission_checksDomainGroupsForDomainUser() {
        AdaptiveUserDetails user = AdaptiveUserDetails.domainUser("user-1", "domain-1", "alice", "a@example.com",
                "hash");
        authenticate(user);

        DomainGroupMember member = new DomainGroupMember();
        member.setDomainGroupId("grp-1");
        member.setDomainId("domain-1");

        DomainGroup group = new DomainGroup();
        group.setId("grp-1");
        group.setDomainId("domain-1");
        group.setPermissions(Set.of(DomainPermission.DOMAIN_MANAGE_APPS));

        when(domainGroupMemberRepository.findByDomainIdAndUserId("domain-1", "user-1"))
                .thenReturn(List.of(member));
        when(domainGroupRepository.findAllById(Set.of("grp-1"))).thenReturn(List.of(group));

        assertTrue(permissionService.hasDomainPermission("domain-1", DomainPermission.DOMAIN_MANAGE_APPS));
        assertFalse(permissionService.hasDomainPermission("domain-1", DomainPermission.DOMAIN_MANAGE_USERS));
    }

    @Test
    void hasAppPermission_evaluatesGroupMembership() {
        AdaptiveUserDetails user = AdaptiveUserDetails.domainUser("user-2", "domain-2", "bob", "b@example.com", "hash");
        authenticate(user);

        AppGroupMember membership = new AppGroupMember();
        membership.setGroupId("app-group-1");
        membership.setAppId("app-123");

        AppGroup adminGroup = new AppGroup();
        adminGroup.setId("app-group-1");
        adminGroup.setAppId("app-123");
        adminGroup.setPermissions(Set.of(AppPermission.APP_READ, AppPermission.APP_WRITE));

        when(appGroupMemberRepository.findByAppIdAndUserId("app-123", "user-2"))
                .thenReturn(List.of(membership));
        when(appGroupRepository.findById("app-group-1")).thenReturn(Optional.of(adminGroup));

        assertTrue(permissionService.hasAppPermission("app-123", AppPermission.APP_WRITE));
        assertFalse(permissionService.hasAppPermission("app-123", AppPermission.APP_EXECUTE));
    }

    private void authenticate(AdaptiveUserDetails principal) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(token);
    }
}
