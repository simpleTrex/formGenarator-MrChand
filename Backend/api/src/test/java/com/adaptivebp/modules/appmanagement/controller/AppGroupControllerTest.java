package com.adaptivebp.modules.appmanagement.controller;

import com.adaptivebp.modules.appmanagement.dto.response.AppGroupMemberResponse;
import com.adaptivebp.modules.appmanagement.dto.response.AppUserResponse;
import com.adaptivebp.modules.organisation.dto.request.AssignMemberRequest;
import com.adaptivebp.modules.appmanagement.model.Application;
import com.adaptivebp.modules.appmanagement.model.AppGroup;
import com.adaptivebp.modules.appmanagement.model.AppGroupMember;
import com.adaptivebp.modules.identity.model.DomainUser;
import com.adaptivebp.modules.organisation.permission.DomainPermission;
import com.adaptivebp.modules.appmanagement.repository.AppGroupMemberRepository;
import com.adaptivebp.modules.appmanagement.repository.AppGroupRepository;
import com.adaptivebp.modules.appmanagement.repository.ApplicationRepository;
import com.adaptivebp.modules.organisation.repository.OrganisationRepository;
import com.adaptivebp.modules.identity.repository.DomainUserRepository;
import com.adaptivebp.modules.organisation.service.PermissionService;
import com.adaptivebp.modules.organisation.model.Organisation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppGroupControllerTest {

    @Mock
    private AppGroupRepository appGroupRepository;

    @Mock
    private AppGroupMemberRepository appGroupMemberRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private OrganisationRepository organisationRepository;

    @Mock
    private DomainUserRepository domainUserRepository;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private AppGroupController appGroupController;

    private Application testApp;
    private AppGroup testGroup;
    private DomainUser testUser;
    private Organisation testOrganisation;
    private String domainSlug = "test-domain";
    private String appSlug = "test-app";
    private String domainId = "domain-123";
    private String appId = "app-123";

    @BeforeEach
    void setUp() {
        testOrganisation = new Organisation();
        testOrganisation.setId(domainId);
        testOrganisation.setSlug(domainSlug);
        testOrganisation.setName("Test Domain");

        testApp = new Application();
        testApp.setId(appId);
        testApp.setSlug(appSlug);
        testApp.setName("Test App");
        testApp.setDomainId(domainId);

        testGroup = new AppGroup();
        testGroup.setId("group-123");
        testGroup.setName("Admins");
        testGroup.setAppId(appId);

        testUser = new DomainUser();
        testUser.setId("user-123");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setDomainId(domainId);
    }

    @Test
    void testListUsersWithGroups_Success() {
        // Arrange
        when(organisationRepository.findBySlug(domainSlug)).thenReturn(Optional.of(testOrganisation));
        when(applicationRepository.findByDomainIdAndSlug(domainId, appSlug))
                .thenReturn(Optional.of(testApp));
        when(permissionService.hasDomainPermission(domainId, DomainPermission.DOMAIN_MANAGE_APPS))
                .thenReturn(true);
        
        List<DomainUser> users = Arrays.asList(testUser);
        when(domainUserRepository.findByDomainId(domainId)).thenReturn(users);

        List<AppGroup> groups = Arrays.asList(testGroup);
        when(appGroupRepository.findByAppId(appId)).thenReturn(groups);

        AppGroupMember member = new AppGroupMember();
        member.setId("member-123");
        member.setGroupId(testGroup.getId());
        member.setUserId(testUser.getId());
        member.setAppId(appId);
        member.setAssignedAt(Instant.now());
        member.setAssignedBy("admin");

        when(appGroupMemberRepository.findByAppIdAndUserId(appId, testUser.getId()))
                .thenReturn(Arrays.asList(member));

        // Act
        ResponseEntity<?> response = appGroupController.listUsersWithGroups(domainSlug, appSlug);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        List<AppUserResponse> usersResponse = (List<AppUserResponse>) response.getBody();
        assertNotNull(usersResponse);
        assertEquals(1, usersResponse.size());
        assertEquals(testUser.getId(), usersResponse.get(0).getId());
        assertEquals(1, usersResponse.get(0).getAppGroups().size());
    }

    @Test
    void testListUsersWithGroups_NoPermission() {
        // Arrange
        when(organisationRepository.findBySlug(domainSlug)).thenReturn(Optional.of(testOrganisation));
        when(applicationRepository.findByDomainIdAndSlug(domainId, appSlug))
                .thenReturn(Optional.of(testApp));
        when(permissionService.hasDomainPermission(domainId, DomainPermission.DOMAIN_MANAGE_APPS))
                .thenReturn(false);

        // Act
        ResponseEntity<?> response = appGroupController.listUsersWithGroups(domainSlug, appSlug);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testListUsersWithGroups_AppNotFound() {
        // Arrange
        when(organisationRepository.findBySlug(domainSlug)).thenReturn(Optional.of(testOrganisation));
        when(applicationRepository.findByDomainIdAndSlug(domainId, appSlug))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
            appGroupController.listUsersWithGroups(domainSlug, appSlug);
        });
    }

    @Test
    void testListGroupMembers_Success() {
        // Arrange
        when(organisationRepository.findBySlug(domainSlug)).thenReturn(Optional.of(testOrganisation));
        when(applicationRepository.findByDomainIdAndSlug(domainId, appSlug))
                .thenReturn(Optional.of(testApp));
        when(permissionService.hasDomainPermission(domainId, DomainPermission.DOMAIN_MANAGE_APPS))
                .thenReturn(true);
        when(appGroupRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));

        AppGroupMember member = new AppGroupMember();
        member.setId("member-123");
        member.setGroupId(testGroup.getId());
        member.setUserId(testUser.getId());
        member.setAppId(appId);
        member.setAssignedAt(Instant.now());
        member.setAssignedBy("admin");

        when(appGroupMemberRepository.findByGroupId(testGroup.getId()))
                .thenReturn(Arrays.asList(member));
        when(domainUserRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        // Act
        ResponseEntity<?> response = appGroupController.listGroupMembers(
                domainSlug, appSlug, testGroup.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        List<AppGroupMemberResponse> members = (List<AppGroupMemberResponse>) response.getBody();
        assertNotNull(members);
        assertEquals(1, members.size());
        assertEquals(testUser.getUsername(), members.get(0).getUsername());
    }

    @Test
    void testListGroupMembers_GroupNotFound() {
        // Arrange
        when(organisationRepository.findBySlug(domainSlug)).thenReturn(Optional.of(testOrganisation));
        when(applicationRepository.findByDomainIdAndSlug(domainId, appSlug))
                .thenReturn(Optional.of(testApp));
        when(appGroupRepository.findById(testGroup.getId())).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = appGroupController.listGroupMembers(
                domainSlug, appSlug, testGroup.getId());

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testListUserGroups_Success() {
        // Arrange
        when(organisationRepository.findBySlug(domainSlug)).thenReturn(Optional.of(testOrganisation));
        when(applicationRepository.findByDomainIdAndSlug(domainId, appSlug))
                .thenReturn(Optional.of(testApp));
        when(permissionService.hasDomainPermission(domainId, DomainPermission.DOMAIN_MANAGE_APPS))
                .thenReturn(true);
        when(domainUserRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        AppGroupMember member = new AppGroupMember();
        member.setId("member-123");
        member.setGroupId(testGroup.getId());
        member.setUserId(testUser.getId());
        member.setAppId(appId);

        when(appGroupMemberRepository.findByAppIdAndUserId(appId, testUser.getId()))
                .thenReturn(Arrays.asList(member));
        when(appGroupRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));

        // Act
        ResponseEntity<?> response = appGroupController.listUserGroups(
                domainSlug, appSlug, testUser.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        List<AppGroup> groups = (List<AppGroup>) response.getBody();
        assertNotNull(groups);
        assertEquals(1, groups.size());
        assertEquals(testGroup.getName(), groups.get(0).getName());
    }

    @Test
    void testListUserGroups_UserNotFound() {
        // Arrange
        when(organisationRepository.findBySlug(domainSlug)).thenReturn(Optional.of(testOrganisation));
        when(applicationRepository.findByDomainIdAndSlug(domainId, appSlug))
                .thenReturn(Optional.of(testApp));
        when(permissionService.hasDomainPermission(domainId, DomainPermission.DOMAIN_MANAGE_APPS))
                .thenReturn(true);
        when(domainUserRepository.findById(testUser.getId())).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = appGroupController.listUserGroups(
                domainSlug, appSlug, testUser.getId());

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testAddMember_Success() {
        // Arrange
        when(organisationRepository.findBySlug(domainSlug)).thenReturn(Optional.of(testOrganisation));
        when(applicationRepository.findByDomainIdAndSlug(domainId, appSlug))
                .thenReturn(Optional.of(testApp));
        when(permissionService.hasDomainPermission(domainId, DomainPermission.DOMAIN_MANAGE_APPS))
                .thenReturn(true);
        when(appGroupRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));
        when(domainUserRepository.findByDomainIdAndUsername(domainId, testUser.getUsername()))
                .thenReturn(Optional.of(testUser));
        when(appGroupMemberRepository.findByGroupIdAndUserId(testGroup.getId(), testUser.getId()))
                .thenReturn(Optional.empty());

        AppGroupMember savedMember = new AppGroupMember();
        savedMember.setId("member-123");
        savedMember.setGroupId(testGroup.getId());
        savedMember.setUserId(testUser.getId());
        savedMember.setAppId(appId);
        when(appGroupMemberRepository.save(any(AppGroupMember.class))).thenReturn(savedMember);

        AssignMemberRequest request = new AssignMemberRequest();
        request.setUsername(testUser.getUsername());

        // Act
        ResponseEntity<?> response = appGroupController.addMember(
                domainSlug, appSlug, testGroup.getId(), request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(appGroupMemberRepository).save(any(AppGroupMember.class));
    }

    @Test
    void testRemoveMember_Success() {
        // Arrange
        when(organisationRepository.findBySlug(domainSlug)).thenReturn(Optional.of(testOrganisation));
        when(applicationRepository.findByDomainIdAndSlug(domainId, appSlug))
                .thenReturn(Optional.of(testApp));
        when(permissionService.hasDomainPermission(domainId, DomainPermission.DOMAIN_MANAGE_APPS))
                .thenReturn(true);
        when(appGroupRepository.findById(testGroup.getId())).thenReturn(Optional.of(testGroup));

        AppGroupMember member = new AppGroupMember();
        member.setId("member-123");
        member.setGroupId(testGroup.getId());
        member.setUserId(testUser.getId());
        member.setAppId(appId);
        when(appGroupMemberRepository.findByGroupIdAndUserId(testGroup.getId(), testUser.getId()))
                .thenReturn(Optional.of(member));

        // Act
        ResponseEntity<?> response = appGroupController.removeMember(
                domainSlug, appSlug, testGroup.getId(), testUser.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(appGroupMemberRepository).delete(member);
    }
}
