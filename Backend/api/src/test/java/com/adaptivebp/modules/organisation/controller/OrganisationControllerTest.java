package com.adaptivebp.modules.organisation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.adaptivebp.modules.organisation.repository.OrganisationRepository;
import com.adaptivebp.modules.organisation.service.DomainProvisioningService;
import com.adaptivebp.shared.security.AdaptiveUserDetails;
import com.adaptivebp.modules.organisation.dto.request.CreateOrganisationRequest;
import com.adaptivebp.modules.organisation.model.Organisation;
import com.adaptivebp.modules.organisation.dto.response.OrganisationResponse;

@ExtendWith(MockitoExtension.class)
class OrganisationControllerTest {

    @Mock
    private OrganisationRepository organisationRepository;

    @Mock
    private DomainProvisioningService domainProvisioningService;

    @InjectMocks
    private OrganisationController organisationController;

    @Test
    void createDomain_shouldSaveAndReturn() {
        CreateOrganisationRequest request = new CreateOrganisationRequest();
        request.setName("acme");
        request.setSlug("acme");

        AdaptiveUserDetails owner = AdaptiveUserDetails.owner("owner-1", "owner@example.com", "hash");
        Authentication auth = new UsernamePasswordAuthenticationToken(owner, null, owner.getAuthorities());

        when(organisationRepository.existsBySlug("acme")).thenReturn(false);
        Organisation saved = new Organisation("acme", "acme", owner.getId());
        saved.setId("d1");
        when(organisationRepository.save(any(Organisation.class))).thenReturn(saved);

        try (MockedStatic<SecurityContextHolder> mocked = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext context = Mockito.mock(SecurityContext.class);
            when(context.getAuthentication()).thenReturn(auth);
            mocked.when(SecurityContextHolder::getContext).thenReturn(context);

            ResponseEntity<?> response = organisationController.createDomain(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            OrganisationResponse body = (OrganisationResponse) response.getBody();
            assertNotNull(body);
            assertEquals("d1", body.getId());
            assertEquals("acme", body.getSlug());
            verify(organisationRepository).save(any(Organisation.class));
            verify(domainProvisioningService).provisionDefaults(saved, null);
        }
    }

    @Test
    void createDomain_domainExists_shouldReturnBadRequest() {
        CreateOrganisationRequest request = new CreateOrganisationRequest();
        request.setName("acme");
        request.setSlug("acme");

        AdaptiveUserDetails owner = AdaptiveUserDetails.owner("owner-1", "owner@example.com", "hash");
        Authentication auth = new UsernamePasswordAuthenticationToken(owner, null, owner.getAuthorities());

        when(organisationRepository.existsBySlug("acme")).thenReturn(true);

        try (MockedStatic<SecurityContextHolder> mocked = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext context = Mockito.mock(SecurityContext.class);
            when(context.getAuthentication()).thenReturn(auth);
            mocked.when(SecurityContextHolder::getContext).thenReturn(context);

            ResponseEntity<?> response = organisationController.createDomain(request);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            verify(organisationRepository).existsBySlug("acme");
        }
    }

    @Test
    void createDomain_shouldReturnForbidden_forDomainUser() {
        CreateOrganisationRequest request = new CreateOrganisationRequest();
        request.setName("acme");

        AdaptiveUserDetails domainUser = AdaptiveUserDetails.domainUser("user-1", "domain-1", "alice", "a@example.com",
                "hash");
        Authentication auth = new UsernamePasswordAuthenticationToken(domainUser, null, domainUser.getAuthorities());

        try (MockedStatic<SecurityContextHolder> mocked = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext context = Mockito.mock(SecurityContext.class);
            when(context.getAuthentication()).thenReturn(auth);
            mocked.when(SecurityContextHolder::getContext).thenReturn(context);

            ResponseEntity<?> response = organisationController.createDomain(request);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    @Test
    void getUserDomains_shouldReturnUserDomains() {
        // Given
        AdaptiveUserDetails owner = AdaptiveUserDetails.owner("owner-1", "owner@example.com", "hash");
        Authentication auth = new UsernamePasswordAuthenticationToken(owner, null, owner.getAuthorities());

        Organisation org1 = new Organisation("acme", "acme", "u1");
        org1.setId("d1");
        org1.setDescription("Test domain 1");
        org1.setIndustry("Tech");

        Organisation org2 = new Organisation("beta", "beta", "u1");
        org2.setId("d2");
        org2.setDescription("Test domain 2");
        org2.setIndustry("Finance");

        List<Organisation> userOrgs = Arrays.asList(org1, org2);

        when(organisationRepository.findByOwnerUserId(owner.getId())).thenReturn(userOrgs);

        try (MockedStatic<SecurityContextHolder> mocked = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext context = Mockito.mock(SecurityContext.class);
            when(context.getAuthentication()).thenReturn(auth);
            mocked.when(SecurityContextHolder::getContext).thenReturn(context);

            ResponseEntity<List<OrganisationResponse>> response = organisationController.getUserDomains();

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            List<OrganisationResponse> orgResponses = response.getBody();
            assertEquals(2, orgResponses.size());

            OrganisationResponse firstOrg = orgResponses.get(0);
            assertEquals("d1", firstOrg.getId());
            assertEquals("acme", firstOrg.getName());
            assertEquals("acme", firstOrg.getSlug());
            assertEquals("u1", firstOrg.getOwnerUserId());
            assertEquals("Test domain 1", firstOrg.getDescription());
            assertEquals("Tech", firstOrg.getIndustry());

            OrganisationResponse secondOrg = orgResponses.get(1);
            assertEquals("d2", secondOrg.getId());
            assertEquals("beta", secondOrg.getName());
            assertEquals("beta", secondOrg.getSlug());
            assertEquals("u1", secondOrg.getOwnerUserId());
            assertEquals("Test domain 2", secondOrg.getDescription());
            assertEquals("Finance", secondOrg.getIndustry());

            verify(organisationRepository).findByOwnerUserId(owner.getId());
        }
    }
}
