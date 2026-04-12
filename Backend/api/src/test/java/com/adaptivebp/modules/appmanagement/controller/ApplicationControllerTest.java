package com.adaptivebp.modules.appmanagement.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.adaptivebp.modules.appmanagement.dto.request.CreateApplicationRequest;
import com.adaptivebp.modules.appmanagement.model.Application;
import com.adaptivebp.modules.appmanagement.permission.AppPermission;
import com.adaptivebp.modules.appmanagement.repository.ApplicationRepository;
import com.adaptivebp.modules.appmanagement.service.ApplicationDeletionService;
import com.adaptivebp.modules.appmanagement.service.ApplicationProvisioningService;
import com.adaptivebp.modules.organisation.model.Organisation;
import com.adaptivebp.modules.organisation.permission.DomainPermission;
import com.adaptivebp.modules.organisation.port.OrganisationLookupPort;
import com.adaptivebp.modules.organisation.service.PermissionService;
import com.adaptivebp.shared.security.AdaptiveUserDetails;

@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {

    @Mock
    private OrganisationLookupPort organisationLookupPort;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private PermissionService permissionService;

    @Mock
    private ApplicationProvisioningService applicationProvisioningService;

    @Mock
    private ApplicationDeletionService applicationDeletionService;

    @InjectMocks
    private ApplicationController controller;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_defaultsOwnerToCurrentUser_whenOwnerUserIdIsBlank() {
        authenticateDomainUser("u1", "d1");
        Organisation org = domain("d1", "acme");

        when(organisationLookupPort.findBySlug("acme")).thenReturn(Optional.of(org));
        when(permissionService.hasDomainPermission("d1", DomainPermission.DOMAIN_MANAGE_APPS)).thenReturn(true);
        when(applicationRepository.existsByDomainIdAndSlug("d1", "sales-app")).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> {
            Application a = invocation.getArgument(0);
            a.setId("app-1");
            return a;
        });

        CreateApplicationRequest req = new CreateApplicationRequest();
        req.setName("Sales");
        req.setSlug("sales-app");
        req.setOwnerUserId("   ");

        ResponseEntity<?> response = controller.create("acme", req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ArgumentCaptor<Application> appCaptor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(appCaptor.capture());
        assertEquals("u1", appCaptor.getValue().getOwnerUserId());
        verify(applicationProvisioningService).provisionDefaultGroups(any(Application.class), eq("u1"), eq("u1"));
    }

    @Test
    void list_includesOwnedApps_whenAppViewPermissionMissing() {
        authenticateDomainUser("u1", "d1");
        Organisation org = domain("d1", "acme");

        Application owned = app("a1", "d1", "owned", "u1");
        Application other = app("a2", "d1", "other", "u2");

        when(organisationLookupPort.findBySlug("acme")).thenReturn(Optional.of(org));
        when(applicationRepository.findByDomainId("d1")).thenReturn(List.of(owned, other));
        when(permissionService.hasAppPermission("a1", AppPermission.APP_VIEW)).thenReturn(false);
        when(permissionService.hasAppPermission("a2", AppPermission.APP_VIEW)).thenReturn(false);

        ResponseEntity<?> response = controller.list("acme");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        List<Application> apps = (List<Application>) response.getBody();
        assertNotNull(apps);
        assertEquals(1, apps.size());
        assertEquals("a1", apps.get(0).getId());
    }

    @Test
    void getApplication_allowsOwner_whenAppViewPermissionMissing() {
        authenticateDomainUser("u1", "d1");
        Organisation org = domain("d1", "acme");
        Application owned = app("a1", "d1", "owned", "u1");

        when(organisationLookupPort.findBySlug("acme")).thenReturn(Optional.of(org));
        when(applicationRepository.findByDomainIdAndSlug("d1", "owned")).thenReturn(Optional.of(owned));
        when(permissionService.hasAppPermission("a1", AppPermission.APP_VIEW)).thenReturn(false);

        ResponseEntity<?> response = controller.getApplication("acme", "owned");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(owned, response.getBody());
    }

    private void authenticateDomainUser(String userId, String domainId) {
        AdaptiveUserDetails principal = AdaptiveUserDetails.domainUser(userId, domainId, "tester", "t@example.com", "hash");
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    private Organisation domain(String id, String slug) {
        Organisation org = new Organisation("Acme", slug, "owner");
        org.setId(id);
        return org;
    }

    private Application app(String id, String domainId, String slug, String ownerUserId) {
        Application app = new Application();
        app.setId(id);
        app.setDomainId(domainId);
        app.setSlug(slug);
        app.setName(slug);
        app.setOwnerUserId(ownerUserId);
        return app;
    }
}
