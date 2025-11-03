package com.formgenerator.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.List;
import java.util.Arrays;

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
import com.formgenerator.api.repository.UserRepository;
import com.formgenerator.platform.auth.CreateDomainRequest;
import com.formgenerator.platform.auth.Domain;
import com.formgenerator.platform.auth.DomainResponse;
import com.formgenerator.platform.auth.User;
import com.formgenerator.platform.auth.UserDetailsImpl;

@ExtendWith(MockitoExtension.class)
class DomainControllerTest {

    @Mock
    private DomainRepository domainRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DomainController domainController;

    @Test
    void createDomain_shouldSaveAndReturn() {
        // Given
        CreateDomainRequest request = new CreateDomainRequest();
        request.setName("acme");
        request.setSlug("acme");

        User user = new User("u1", "u1@example.com", "pw");
        user.setId("u1");
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(domainRepository.existsBySlug("acme")).thenReturn(false);
        Domain saved = new Domain("acme", "acme", "u1");
        saved.setId("d1");
        when(domainRepository.save(any(Domain.class))).thenReturn(saved);

        // Mock SecurityContextHolder
        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext context = Mockito.mock(SecurityContext.class);
            when(context.getAuthentication()).thenReturn(auth);
            mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(context);

            // When
            ResponseEntity<?> response = domainController.createDomain(request);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            
            DomainResponse domainResponse = (DomainResponse) response.getBody();
            assertEquals("d1", domainResponse.getId());
            assertEquals("acme", domainResponse.getName());
            assertEquals("acme", domainResponse.getSlug());
            assertEquals("u1", domainResponse.getOwnerUserId());

            verify(domainRepository).save(any(Domain.class));
            verify(userRepository).findById("u1");
            verify(domainRepository).existsBySlug("acme");
        }
    }

    @Test
    void createDomain_domainExists_shouldReturnBadRequest() {
        // Given
        CreateDomainRequest request = new CreateDomainRequest();
        request.setName("acme");
        request.setSlug("acme");

        User user = new User("u1", "u1@example.com", "pw");
        user.setId("u1");
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(domainRepository.existsBySlug("acme")).thenReturn(true);

        // Mock SecurityContextHolder
        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext context = Mockito.mock(SecurityContext.class);
            when(context.getAuthentication()).thenReturn(auth);
            mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(context);

            // When
            ResponseEntity<?> response = domainController.createDomain(request);

            // Then
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            verify(domainRepository).existsBySlug("acme");
        }
    }

    @Test
    void createDomain_userNotFound_shouldReturnBadRequest() {
        // Given
        CreateDomainRequest request = new CreateDomainRequest();
        request.setName("acme");

        User user = new User("u1", "u1@example.com", "pw");
        user.setId("u1");
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(userRepository.findById("u1")).thenReturn(Optional.empty());

        // Mock SecurityContextHolder
        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext context = Mockito.mock(SecurityContext.class);
            when(context.getAuthentication()).thenReturn(auth);
            mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(context);

            // When
            ResponseEntity<?> response = domainController.createDomain(request);

            // Then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
        }
    }

    @Test
    void getUserDomains_shouldReturnUserDomains() {
        // Given
        User user = new User("u1", "u1@example.com", "pw");
        user.setId("u1");
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        Domain domain1 = new Domain("acme", "acme", "u1");
        domain1.setId("d1");
        domain1.setDescription("Test domain 1");
        domain1.setIndustry("Tech");

        Domain domain2 = new Domain("beta", "beta", "u1");
        domain2.setId("d2");
        domain2.setDescription("Test domain 2");
        domain2.setIndustry("Finance");

        List<Domain> userDomains = Arrays.asList(domain1, domain2);

        when(domainRepository.findByOwnerUserId("u1")).thenReturn(userDomains);

        // Mock SecurityContextHolder
        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext context = Mockito.mock(SecurityContext.class);
            when(context.getAuthentication()).thenReturn(auth);
            mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(context);

            // When
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

            verify(domainRepository).findByOwnerUserId("u1");
        }
    }
}
