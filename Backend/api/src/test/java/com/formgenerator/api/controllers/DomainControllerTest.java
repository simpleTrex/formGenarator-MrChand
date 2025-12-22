package com.formgenerator.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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

import com.formgenerator.api.repository.DomainRepository;
import com.formgenerator.api.services.DomainProvisioningService;
import com.formgenerator.platform.auth.AdaptiveUserDetails;
import com.formgenerator.platform.auth.CreateDomainRequest;
import com.formgenerator.platform.auth.Domain;
import com.formgenerator.platform.auth.DomainResponse;

@ExtendWith(MockitoExtension.class)
class DomainControllerTest {

    @Mock
    private DomainRepository domainRepository;

    @Mock
    private DomainProvisioningService domainProvisioningService;

    @InjectMocks
    private DomainController domainController;

    @Test
    void createDomain_shouldSaveAndReturn() {
        CreateDomainRequest request = new CreateDomainRequest();
        request.setName("acme");
        request.setSlug("acme");

        AdaptiveUserDetails owner = AdaptiveUserDetails.owner("owner-1", "owner@example.com", "hash");
        Authentication auth = new UsernamePasswordAuthenticationToken(owner, null, owner.getAuthorities());

        when(domainRepository.existsBySlug("acme")).thenReturn(false);
        Domain saved = new Domain("acme", "acme", owner.getId());
        saved.setId("d1");
        when(domainRepository.save(any(Domain.class))).thenReturn(saved);

        try (MockedStatic<SecurityContextHolder> mocked = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext context = Mockito.mock(SecurityContext.class);
            when(context.getAuthentication()).thenReturn(auth);
            mocked.when(SecurityContextHolder::getContext).thenReturn(context);

            ResponseEntity<?> response = domainController.createDomain(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            DomainResponse body = (DomainResponse) response.getBody();
            assertNotNull(body);
            assertEquals("d1", body.getId());
            assertEquals("acme", body.getSlug());
            verify(domainRepository).save(any(Domain.class));
            verify(domainProvisioningService).provisionDefaults(saved, null);
        }
    }

    @Test
    void createDomain_domainExists_shouldReturnBadRequest() {
        CreateDomainRequest request = new CreateDomainRequest();
        request.setName("acme");
        request.setSlug("acme");

        AdaptiveUserDetails owner = AdaptiveUserDetails.owner("owner-1", "owner@example.com", "hash");
        Authentication auth = new UsernamePasswordAuthenticationToken(owner, null, owner.getAuthorities());

        when(domainRepository.existsBySlug("acme")).thenReturn(true);

        try (MockedStatic<SecurityContextHolder> mocked = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext context = Mockito.mock(SecurityContext.class);
            when(context.getAuthentication()).thenReturn(auth);
            mocked.when(SecurityContextHolder::getContext).thenReturn(context);

            ResponseEntity<?> response = domainController.createDomain(request);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            verify(domainRepository).existsBySlug("acme");
        }
    }

    @Test
    void createDomain_shouldReturnForbidden_forDomainUser() {
        CreateDomainRequest request = new CreateDomainRequest();
        request.setName("acme");

        AdaptiveUserDetails domainUser = AdaptiveUserDetails.domainUser("user-1", "domain-1", "alice", "a@example.com",
                "hash");
        Authentication auth = new UsernamePasswordAuthenticationToken(domainUser, null, domainUser.getAuthorities());

        try (MockedStatic<SecurityContextHolder> mocked = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext context = Mockito.mock(SecurityContext.class);
            when(context.getAuthentication()).thenReturn(auth);
            mocked.when(SecurityContextHolder::getContext).thenReturn(context);

            ResponseEntity<?> response = domainController.createDomain(request);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    @Test
    void getUserDomains_shouldReturnUserDomains() {
        // Given
        AdaptiveUserDetails owner = AdaptiveUserDetails.owner("owner-1", "owner@example.com", "hash");
        Authentication auth = new UsernamePasswordAuthenticationToken(owner, null, owner.getAuthorities());

        Domain domain1 = new Domain("acme", "acme", "u1");
        domain1.setId("d1");
        domain1.setDescription("Test domain 1");
        domain1.setIndustry("Tech");

        Domain domain2 = new Domain("beta", "beta", "u1");
        domain2.setId("d2");
        domain2.setDescription("Test domain 2");
        domain2.setIndustry("Finance");

        List<Domain> userDomains = Arrays.asList(domain1, domain2);

        when(domainRepository.findByOwnerUserId(owner.getId())).thenReturn(userDomains);

        try (MockedStatic<SecurityContextHolder> mocked = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext context = Mockito.mock(SecurityContext.class);
            when(context.getAuthentication()).thenReturn(auth);
            mocked.when(SecurityContextHolder::getContext).thenReturn(context);

            ResponseEntity<List<DomainResponse>> response = domainController.getUserDomains();

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            List<DomainResponse> domainResponses = response.getBody();
            assertEquals(2, domainResponses.size());

            DomainResponse firstDomain = domainResponses.get(0);
            assertEquals("d1", firstDomain.getId());
            assertEquals("acme", firstDomain.getName());
            assertEquals("acme", firstDomain.getSlug());
            assertEquals("u1", firstDomain.getOwnerUserId());
            assertEquals("Test domain 1", firstDomain.getDescription());
            assertEquals("Tech", firstDomain.getIndustry());

            DomainResponse secondDomain = domainResponses.get(1);
            assertEquals("d2", secondDomain.getId());
            assertEquals("beta", secondDomain.getName());
            assertEquals("beta", secondDomain.getSlug());
            assertEquals("u1", secondDomain.getOwnerUserId());
            assertEquals("Test domain 2", secondDomain.getDescription());
            assertEquals("Finance", secondDomain.getIndustry());

            verify(domainRepository).findByOwnerUserId(owner.getId());
        }
    }
}
