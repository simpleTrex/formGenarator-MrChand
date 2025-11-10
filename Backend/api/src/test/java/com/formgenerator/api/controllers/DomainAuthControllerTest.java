package com.formgenerator.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.formgenerator.api.dto.auth.DomainLoginRequest;
import com.formgenerator.api.models.domain.DomainUser;
import com.formgenerator.api.repository.DomainRepository;
import com.formgenerator.api.repository.DomainUserRepository;
import com.formgenerator.platform.auth.Domain;
import com.formgenerator.platform.auth.JwtUtils;

@ExtendWith(MockitoExtension.class)
class DomainAuthControllerTest {

    @Mock
    private DomainRepository domainRepository;

    @Mock
    private DomainUserRepository domainUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private DomainAuthController controller;

    private Domain domain;
    private DomainUser domainUser;

    @BeforeEach
    void setup() {
        domain = new Domain("Acme", "acme", "owner-1");
        domain.setId("domain-1");
        domainUser = new DomainUser();
        domainUser.setId("user-1");
        domainUser.setDomainId(domain.getId());
        domainUser.setUsername("alice");
        domainUser.setEmail("alice@acme.com");
        domainUser.setPasswordHash("hash");
    }

    @Test
    void login_shouldReturnToken_whenCredentialsValid() {
        DomainLoginRequest request = new DomainLoginRequest();
        request.setUsername("alice");
        request.setPassword("secret");

        when(domainRepository.findBySlug("acme")).thenReturn(Optional.of(domain));
        when(domainUserRepository.findByDomainIdAndUsername(domain.getId(), request.getUsername()))
                .thenReturn(Optional.of(domainUser));
        when(passwordEncoder.matches(request.getPassword(), domainUser.getPasswordHash())).thenReturn(true);
        ResponseCookie cookie = ResponseCookie.from("token", "xyz").build();
        when(jwtUtils.generateDomainJwtCookie(any())).thenReturn(cookie);

        ResponseEntity<?> response = controller.login("acme", request);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).contains("token=xyz"));
    }

    @Test
    void login_shouldFail_whenDomainMissing() {
        when(domainRepository.findBySlug("acme")).thenReturn(Optional.empty());
        DomainLoginRequest request = new DomainLoginRequest();
        request.setUsername("any");
        request.setPassword("pw");

        ResponseEntity<?> response = controller.login("acme", request);

        assertEquals(404, response.getStatusCode().value());
    }
}
