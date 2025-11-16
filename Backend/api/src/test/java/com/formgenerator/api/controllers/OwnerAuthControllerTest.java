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

import com.formgenerator.api.dto.auth.OwnerLoginRequest;
import com.formgenerator.api.models.owner.OwnerAccount;
import com.formgenerator.api.repository.OwnerAccountRepository;
import com.formgenerator.platform.auth.JwtUtils;

@ExtendWith(MockitoExtension.class)
class OwnerAuthControllerTest {

    @Mock
    private OwnerAccountRepository ownerAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private OwnerAuthController controller;

    private OwnerAccount account;

    @BeforeEach
    void setup() {
        account = new OwnerAccount();
        account.setId("owner-1");
        account.setEmail("owner@example.com");
        account.setPasswordHash("hashed");
    }

    @Test
    void login_shouldReturnAuthResponse_whenCredentialsValid() {
        OwnerLoginRequest request = new OwnerLoginRequest();
        request.setEmail(account.getEmail());
        request.setPassword("secret");

        when(ownerAccountRepository.findByEmail(account.getEmail())).thenReturn(Optional.of(account));
        when(passwordEncoder.matches(request.getPassword(), account.getPasswordHash())).thenReturn(true);
        ResponseCookie cookie = ResponseCookie.from("token", "abc").build();
        when(jwtUtils.generateOwnerJwtCookie(any())).thenReturn(cookie);

        ResponseEntity<?> response = controller.login(request);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).contains("token=abc"));
    }

    @Test
    void login_shouldReturnUnauthorized_whenCredentialsInvalid() {
        OwnerLoginRequest request = new OwnerLoginRequest();
        request.setEmail(account.getEmail());
        request.setPassword("bad");

        when(ownerAccountRepository.findByEmail(account.getEmail())).thenReturn(Optional.of(account));
        when(passwordEncoder.matches(request.getPassword(), account.getPasswordHash())).thenReturn(false);

        ResponseEntity<?> response = controller.login(request);

        assertEquals(401, response.getStatusCode().value());
    }
}
