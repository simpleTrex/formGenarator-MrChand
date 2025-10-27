package com.formgenerator.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.formgenerator.api.repository.RoleRepository;
import com.formgenerator.api.repository.UserRepository;
import com.formgenerator.api.repository.DomainRepository;
import com.formgenerator.platform.auth.ERole;
import com.formgenerator.platform.auth.MessageResponse;
import com.formgenerator.platform.auth.Role;
import com.formgenerator.platform.auth.SignupRequest;
import com.formgenerator.platform.auth.User;
import com.formgenerator.platform.auth.Domain;

@ExtendWith(MockitoExtension.class)
public class AuthControllerCanonicalRoleTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private DomainRepository domainRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthController authController;

    @Test
    void signupWithCanonicalRole_shouldAcceptBusinessOwner() {
        // Setup
    Role businessOwnerRole = new Role();
    businessOwnerRole.setId("000000000000000000000001");
        businessOwnerRole.setRoleName("BUSINESS_OWNER");
        businessOwnerRole.setName(ERole.ROLE_BUSINESS_OWNER);

        User savedUser = new User("testuser", "test@example.com", "encoded_password");
        savedUser.setId("mockUserId");

        Domain createdDomain = new Domain("testuser", "mockUserId");
        createdDomain.setId("mockDomainId");

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByRoleName("BUSINESS_OWNER")).thenReturn(Optional.of(businessOwnerRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any())).thenReturn(savedUser);
        when(domainRepository.existsByName("testuser")).thenReturn(false);
        when(domainRepository.save(any(Domain.class))).thenReturn(createdDomain);

        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("testuser");
        signupRequest.setEmail("test@example.com");
        signupRequest.setPassword("password123");
        signupRequest.setRoles(Set.of("BUSINESS_OWNER"));

        // Test
        ResponseEntity<?> response = authController.registerUser(signupRequest);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertEquals("User registered successfully!", messageResponse.getMsg());
    }

    @Test
    void signupWithOldRoleName_shouldMapToCanonical() {
        // Setup
    Role domainAdminRole = new Role();
    domainAdminRole.setId("000000000000000000000002");
        domainAdminRole.setRoleName("DOMAIN_ADMIN");
        domainAdminRole.setName(ERole.ROLE_DOMAIN_ADMIN);

        User savedUser = new User("adminuser", "admin@example.com", "encoded_password");
        savedUser.setId("mockUserId");

        Domain globalDomain = new Domain("global", "someOwnerId");
        globalDomain.setId("globalDomainId");

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByRoleName("DOMAIN_ADMIN")).thenReturn(Optional.of(domainAdminRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any())).thenReturn(savedUser);
        when(domainRepository.findByName("global")).thenReturn(Optional.of(globalDomain));

        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("adminuser");
        signupRequest.setEmail("admin@example.com");
        signupRequest.setPassword("password123");
        signupRequest.setRoles(Set.of("admin")); // Old role name

        // Test
        ResponseEntity<?> response = authController.registerUser(signupRequest);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertEquals("User registered successfully!", messageResponse.getMsg());
    }

    @Test
    void signupWithoutRoles_shouldUseDefaultBusinessUser() {
        // Setup
    Role businessUserRole = new Role();
    businessUserRole.setId("000000000000000000000003");
        businessUserRole.setRoleName("BUSINESS_USER");
        businessUserRole.setName(ERole.ROLE_BUSINESS_USER);

        User savedUser = new User("regularuser", "user@example.com", "encoded_password");
        savedUser.setId("mockUserId");

        Domain globalDomain = new Domain("global", "someOwnerId");
        globalDomain.setId("globalDomainId");

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByRoleName("BUSINESS_USER")).thenReturn(Optional.of(businessUserRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any())).thenReturn(savedUser);
        when(domainRepository.findByName("global")).thenReturn(Optional.of(globalDomain));

        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("regularuser");
        signupRequest.setEmail("user@example.com");
        signupRequest.setPassword("password123");
        // No roles specified

        // Test
        ResponseEntity<?> response = authController.registerUser(signupRequest);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertEquals("User registered successfully!", messageResponse.getMsg());
    }
}