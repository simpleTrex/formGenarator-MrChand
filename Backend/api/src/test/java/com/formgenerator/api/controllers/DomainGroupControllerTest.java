package com.formgenerator.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.formgenerator.api.dto.rbac.AssignMemberRequest;
import com.formgenerator.api.dto.rbac.DomainUserResponse;
import com.formgenerator.api.dto.rbac.GroupMemberResponse;
import com.formgenerator.api.models.domain.DomainGroup;
import com.formgenerator.api.models.domain.DomainGroupMember;
import com.formgenerator.api.models.domain.DomainUser;
import com.formgenerator.api.permissions.DomainPermission;
import com.formgenerator.api.repository.DomainGroupMemberRepository;
import com.formgenerator.api.repository.DomainGroupRepository;
import com.formgenerator.api.repository.DomainRepository;
import com.formgenerator.api.repository.DomainUserRepository;
import com.formgenerator.api.services.PermissionService;
import com.formgenerator.platform.auth.Domain;

@ExtendWith(MockitoExtension.class)
class DomainGroupControllerTest {

    @Mock
    private DomainRepository domainRepository;

    @Mock
    private DomainGroupRepository domainGroupRepository;

    @Mock
    private DomainGroupMemberRepository domainGroupMemberRepository;

    @Mock
    private DomainUserRepository domainUserRepository;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private DomainGroupController domainGroupController;

    @Test
    void listUsersWithGroups_shouldReturnUsersWithTheirGroups() {
        // Arrange
        String slug = "test-domain";
        Domain domain = new Domain("Test Domain", slug, "owner-1");
        domain.setId("domain-1");

        DomainUser user1 = new DomainUser();
        user1.setId("user-1");
        user1.setUsername("john.doe");
        user1.setEmail("john@example.com");
        user1.setStatus("active");
        user1.setDomainId("domain-1");
        user1.setProfile(new HashMap<>());
        user1.setCreatedAt(Instant.now());

        DomainUser user2 = new DomainUser();
        user2.setId("user-2");
        user2.setUsername("jane.smith");
        user2.setEmail("jane@example.com");
        user2.setStatus("active");
        user2.setDomainId("domain-1");
        user2.setProfile(new HashMap<>());
        user2.setCreatedAt(Instant.now());

        DomainGroup adminGroup = new DomainGroup();
        adminGroup.setId("group-1");
        adminGroup.setName("Domain Admin");
        adminGroup.setDomainId("domain-1");
        adminGroup.setPermissions(EnumSet.allOf(DomainPermission.class));

        DomainGroup contributorGroup = new DomainGroup();
        contributorGroup.setId("group-2");
        contributorGroup.setName("Domain Contributor");
        contributorGroup.setDomainId("domain-1");
        contributorGroup.setPermissions(EnumSet.of(DomainPermission.DOMAIN_USE_APP));

        DomainGroupMember membership1 = new DomainGroupMember();
        membership1.setId("member-1");
        membership1.setDomainGroupId("group-1");
        membership1.setDomainId("domain-1");
        membership1.setUserId("user-1");
        membership1.setAssignedAt(Instant.now());

        DomainGroupMember membership2 = new DomainGroupMember();
        membership2.setId("member-2");
        membership2.setDomainGroupId("group-2");
        membership2.setDomainId("domain-1");
        membership2.setUserId("user-2");
        membership2.setAssignedAt(Instant.now());

        when(domainRepository.findBySlug(slug)).thenReturn(Optional.of(domain));
        when(permissionService.hasDomainPermission("domain-1", DomainPermission.DOMAIN_MANAGE_USERS))
                .thenReturn(true);
        when(domainUserRepository.findByDomainId("domain-1")).thenReturn(Arrays.asList(user1, user2));
        when(domainGroupRepository.findByDomainId("domain-1"))
                .thenReturn(Arrays.asList(adminGroup, contributorGroup));
        when(domainGroupMemberRepository.findByDomainIdAndUserId("domain-1", "user-1"))
                .thenReturn(Arrays.asList(membership1));
        when(domainGroupMemberRepository.findByDomainIdAndUserId("domain-1", "user-2"))
                .thenReturn(Arrays.asList(membership2));

        // Act
        ResponseEntity<?> response = domainGroupController.listUsersWithGroups(slug);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        List<DomainUserResponse> users = (List<DomainUserResponse>) response.getBody();
        assertEquals(2, users.size());
        
        DomainUserResponse userResponse1 = users.get(0);
        assertEquals("user-1", userResponse1.getId());
        assertEquals("john.doe", userResponse1.getUsername());
        assertEquals(1, userResponse1.getGroups().size());
        assertEquals("Domain Admin", userResponse1.getGroups().get(0).getGroupName());
        
        DomainUserResponse userResponse2 = users.get(1);
        assertEquals("user-2", userResponse2.getId());
        assertEquals(1, userResponse2.getGroups().size());
        assertEquals("Domain Contributor", userResponse2.getGroups().get(0).getGroupName());

        verify(domainUserRepository).findByDomainId("domain-1");
        verify(domainGroupRepository).findByDomainId("domain-1");
    }

    @Test
    void listUsersWithGroups_noPermission_shouldReturn403() {
        // Arrange
        String slug = "test-domain";
        Domain domain = new Domain("Test Domain", slug, "owner-1");
        domain.setId("domain-1");

        when(domainRepository.findBySlug(slug)).thenReturn(Optional.of(domain));
        when(permissionService.hasDomainPermission("domain-1", DomainPermission.DOMAIN_MANAGE_USERS))
                .thenReturn(false);

        // Act
        ResponseEntity<?> response = domainGroupController.listUsersWithGroups(slug);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void listGroupMembers_shouldReturnMembersOfGroup() {
        // Arrange
        String slug = "test-domain";
        String groupId = "group-1";
        
        Domain domain = new Domain("Test Domain", slug, "owner-1");
        domain.setId("domain-1");

        DomainGroup group = new DomainGroup();
        group.setId(groupId);
        group.setName("Domain Admin");
        group.setDomainId("domain-1");

        DomainUser user1 = new DomainUser();
        user1.setId("user-1");
        user1.setUsername("john.doe");
        user1.setEmail("john@example.com");
        user1.setStatus("active");

        DomainUser user2 = new DomainUser();
        user2.setId("user-2");
        user2.setUsername("jane.smith");
        user2.setEmail("jane@example.com");
        user2.setStatus("active");

        DomainGroupMember member1 = new DomainGroupMember();
        member1.setUserId("user-1");
        member1.setDomainGroupId(groupId);
        member1.setAssignedAt(Instant.now());
        member1.setAssignedBy("admin-1");

        DomainGroupMember member2 = new DomainGroupMember();
        member2.setUserId("user-2");
        member2.setDomainGroupId(groupId);
        member2.setAssignedAt(Instant.now());
        member2.setAssignedBy("admin-1");

        when(domainRepository.findBySlug(slug)).thenReturn(Optional.of(domain));
        when(domainGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(permissionService.hasDomainPermission("domain-1", DomainPermission.DOMAIN_MANAGE_USERS))
                .thenReturn(true);
        when(domainGroupMemberRepository.findByDomainGroupId(groupId))
                .thenReturn(Arrays.asList(member1, member2));
        when(domainUserRepository.findById("user-1")).thenReturn(Optional.of(user1));
        when(domainUserRepository.findById("user-2")).thenReturn(Optional.of(user2));

        // Act
        ResponseEntity<?> response = domainGroupController.listGroupMembers(slug, groupId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        List<GroupMemberResponse> members = (List<GroupMemberResponse>) response.getBody();
        assertEquals(2, members.size());
        assertEquals("user-1", members.get(0).getUserId());
        assertEquals("john.doe", members.get(0).getUsername());
        assertEquals("user-2", members.get(1).getUserId());
        assertEquals("jane.smith", members.get(1).getUsername());

        verify(domainGroupMemberRepository).findByDomainGroupId(groupId);
    }

    @Test
    void listGroupMembers_groupNotFound_shouldReturn404() {
        // Arrange
        String slug = "test-domain";
        String groupId = "invalid-group";
        
        Domain domain = new Domain("Test Domain", slug, "owner-1");
        domain.setId("domain-1");

        when(domainRepository.findBySlug(slug)).thenReturn(Optional.of(domain));
        when(domainGroupRepository.findById(groupId)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = domainGroupController.listGroupMembers(slug, groupId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void listUserGroups_shouldReturnGroupsForUser() {
        // Arrange
        String slug = "test-domain";
        String userId = "user-1";
        
        Domain domain = new Domain("Test Domain", slug, "owner-1");
        domain.setId("domain-1");

        DomainUser user = new DomainUser();
        user.setId(userId);
        user.setUsername("john.doe");
        user.setDomainId("domain-1");

        DomainGroup adminGroup = new DomainGroup();
        adminGroup.setId("group-1");
        adminGroup.setName("Domain Admin");
        adminGroup.setDomainId("domain-1");

        DomainGroup contributorGroup = new DomainGroup();
        contributorGroup.setId("group-2");
        contributorGroup.setName("Domain Contributor");
        contributorGroup.setDomainId("domain-1");

        DomainGroupMember membership1 = new DomainGroupMember();
        membership1.setDomainGroupId("group-1");
        membership1.setUserId(userId);

        DomainGroupMember membership2 = new DomainGroupMember();
        membership2.setDomainGroupId("group-2");
        membership2.setUserId(userId);

        when(domainRepository.findBySlug(slug)).thenReturn(Optional.of(domain));
        when(permissionService.hasDomainPermission("domain-1", DomainPermission.DOMAIN_MANAGE_USERS))
                .thenReturn(true);
        when(domainUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(domainGroupMemberRepository.findByDomainIdAndUserId("domain-1", userId))
                .thenReturn(Arrays.asList(membership1, membership2));
        when(domainGroupRepository.findById("group-1")).thenReturn(Optional.of(adminGroup));
        when(domainGroupRepository.findById("group-2")).thenReturn(Optional.of(contributorGroup));

        // Act
        ResponseEntity<?> response = domainGroupController.listUserGroups(slug, userId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        List<DomainGroup> groups = (List<DomainGroup>) response.getBody();
        assertEquals(2, groups.size());
        assertEquals("Domain Admin", groups.get(0).getName());
        assertEquals("Domain Contributor", groups.get(1).getName());

        verify(domainGroupMemberRepository).findByDomainIdAndUserId("domain-1", userId);
    }

    @Test
    void listUserGroups_userNotFound_shouldReturn404() {
        // Arrange
        String slug = "test-domain";
        String userId = "invalid-user";
        
        Domain domain = new Domain("Test Domain", slug, "owner-1");
        domain.setId("domain-1");

        when(domainRepository.findBySlug(slug)).thenReturn(Optional.of(domain));
        when(permissionService.hasDomainPermission("domain-1", DomainPermission.DOMAIN_MANAGE_USERS))
                .thenReturn(true);
        when(domainUserRepository.findById(userId)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = domainGroupController.listUserGroups(slug, userId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void addMember_shouldAssignUserToGroup() {
        // Arrange
        String slug = "test-domain";
        String groupId = "group-1";
        
        Domain domain = new Domain("Test Domain", slug, "owner-1");
        domain.setId("domain-1");

        DomainGroup group = new DomainGroup();
        group.setId(groupId);
        group.setDomainId("domain-1");

        DomainUser user = new DomainUser();
        user.setId("user-1");
        user.setUsername("john.doe");

        AssignMemberRequest request = new AssignMemberRequest();
        request.setUsername("john.doe");

        when(domainRepository.findBySlug(slug)).thenReturn(Optional.of(domain));
        when(domainGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(permissionService.hasDomainPermission("domain-1", DomainPermission.DOMAIN_MANAGE_USERS))
                .thenReturn(true);
        when(domainUserRepository.findByDomainIdAndUsername("domain-1", "john.doe"))
                .thenReturn(Optional.of(user));
        when(domainGroupMemberRepository.findByDomainGroupIdAndUserId(groupId, "user-1"))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = domainGroupController.addMember(slug, groupId, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(domainGroupMemberRepository).save(any(DomainGroupMember.class));
    }

    @Test
    void removeMember_shouldRemoveUserFromGroup() {
        // Arrange
        String slug = "test-domain";
        String groupId = "group-1";
        String userId = "user-1";
        
        Domain domain = new Domain("Test Domain", slug, "owner-1");
        domain.setId("domain-1");

        DomainGroup group = new DomainGroup();
        group.setId(groupId);
        group.setDomainId("domain-1");

        DomainGroupMember member = new DomainGroupMember();
        member.setId("member-1");
        member.setDomainGroupId(groupId);
        member.setUserId(userId);

        when(domainRepository.findBySlug(slug)).thenReturn(Optional.of(domain));
        when(domainGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(permissionService.hasDomainPermission("domain-1", DomainPermission.DOMAIN_MANAGE_USERS))
                .thenReturn(true);
        when(domainGroupMemberRepository.findByDomainGroupIdAndUserId(groupId, userId))
                .thenReturn(Optional.of(member));

        // Act
        ResponseEntity<?> response = domainGroupController.removeMember(slug, groupId, userId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(domainGroupMemberRepository).delete(member);
    }
}
