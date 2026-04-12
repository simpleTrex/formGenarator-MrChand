package com.adaptivebp.modules.organisation.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.adaptivebp.modules.organisation.model.DomainGroup;
import com.adaptivebp.modules.organisation.model.DomainGroupMember;
import com.adaptivebp.modules.appmanagement.permission.AppPermission;
import com.adaptivebp.modules.appmanagement.port.AppGroupQueryPort;
import com.adaptivebp.modules.organisation.permission.DomainPermission;
import com.adaptivebp.modules.organisation.repository.DomainGroupMemberRepository;
import com.adaptivebp.modules.organisation.repository.DomainGroupRepository;
import com.adaptivebp.shared.security.AdaptiveUserDetails;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private DomainGroupMemberRepository domainGroupMemberRepository;

    @Mock
    private DomainGroupRepository domainGroupRepository;

    @Mock
    private AppGroupQueryPort appGroupQueryPort;

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

        when(appGroupQueryPort.getAppPermissions("app-123", "user-2"))
            .thenReturn(Set.of(AppPermission.APP_READ, AppPermission.APP_WRITE));

        assertTrue(permissionService.hasAppPermission("app-123", AppPermission.APP_WRITE));
        assertFalse(permissionService.hasAppPermission("app-123", AppPermission.APP_EXECUTE));
    }

    @Test
    void hasAppPermission_startWorkflow_allowsViewPermissionCompatibility() {
        AdaptiveUserDetails user = AdaptiveUserDetails.domainUser("user-3", "domain-2", "eve", "e@example.com", "hash");
        authenticate(user);

        when(appGroupQueryPort.getAppPermissions("app-123", "user-3"))
                .thenReturn(Set.of(AppPermission.APP_VIEW));

        assertTrue(permissionService.hasAppPermission("app-123", AppPermission.APP_START_WORKFLOW));
    }

    @Test
    void hasAppPermission_startWorkflow_allowsReadPermissionCompatibility() {
        AdaptiveUserDetails user = AdaptiveUserDetails.domainUser("user-4", "domain-2", "john", "j@example.com", "hash");
        authenticate(user);

        when(appGroupQueryPort.getAppPermissions("app-123", "user-4"))
                .thenReturn(Set.of(AppPermission.APP_READ));

        assertTrue(permissionService.hasAppPermission("app-123", AppPermission.APP_START_WORKFLOW));
    }

    private void authenticate(AdaptiveUserDetails principal) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(token);
    }
}
