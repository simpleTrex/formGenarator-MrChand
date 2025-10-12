package com.formgenerator.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

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

        User user = new User("u1", "u1@example.com", "pw");
        user.setId("u1");
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(domainRepository.existsByName("acme")).thenReturn(false);
        Domain saved = new Domain("acme", "u1");
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
            assertEquals("u1", domainResponse.getOwnerUserId());

            verify(domainRepository).save(any(Domain.class));
        }
    }

    @Test
    void createDomain_domainExists_shouldReturnBadRequest() {
        // Given
        CreateDomainRequest request = new CreateDomainRequest();
        request.setName("acme");

        User user = new User("u1", "u1@example.com", "pw");
        user.setId("u1");
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(domainRepository.existsByName("acme")).thenReturn(true);

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
}
