package com.formgenerator.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.formgenerator.api.dto.auth.AuthResponse;
import com.formgenerator.api.dto.auth.OwnerLoginRequest;
import com.formgenerator.api.dto.auth.OwnerSignupRequest;
import com.formgenerator.api.models.owner.OwnerAccount;
import com.formgenerator.api.repository.OwnerAccountRepository;
import com.formgenerator.platform.auth.AdaptiveUserDetails;
import com.formgenerator.platform.auth.PrincipalType;
import com.formgenerator.platform.auth.JwtUtils;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/adaptive/auth/owner")
@Validated
public class OwnerAuthController {

    @Autowired
    private OwnerAccountRepository ownerAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody OwnerSignupRequest request) {
        if (ownerAccountRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email already registered");
        }
        OwnerAccount account = new OwnerAccount();
        account.setEmail(request.getEmail().toLowerCase());
        account.setDisplayName(request.getDisplayName());
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setStatus("ACTIVE");
        ownerAccountRepository.save(account);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody OwnerLoginRequest request) {
        return ownerAccountRepository.findByEmail(request.getEmail().toLowerCase())
                .map(account -> {
                    if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
                        return ResponseEntity.status(401).body("Invalid credentials");
                    }
                    return buildOwnerLoginResponse(account);
                })
                .orElseGet(() -> ResponseEntity.status(401).body("Invalid credentials"));
    }

    private ResponseEntity<AuthResponse> buildOwnerLoginResponse(OwnerAccount account) {
        AdaptiveUserDetails principal = AdaptiveUserDetails.owner(account.getId(), account.getEmail(),
                account.getPasswordHash());
        ResponseCookie cookie = jwtUtils.generateOwnerJwtCookie(principal);
        AuthResponse response = new AuthResponse(account.getId(), null, PrincipalType.OWNER,
                cookie.getValue());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(response);
    }
}
