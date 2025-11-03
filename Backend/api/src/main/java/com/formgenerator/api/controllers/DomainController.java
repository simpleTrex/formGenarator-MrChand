package com.formgenerator.api.controllers;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.formgenerator.api.repository.DomainRepository;
import com.formgenerator.api.repository.UserRepository;
import com.formgenerator.platform.auth.CreateDomainRequest;
import com.formgenerator.platform.auth.Domain;
import com.formgenerator.platform.auth.DomainResponse;
import com.formgenerator.platform.auth.MessageResponse;
import com.formgenerator.platform.auth.User;
import com.formgenerator.platform.auth.UserDetailsImpl;

import jakarta.validation.Valid;

/**
 * REST Controller for Domain management operations.
 * Handles domain creation, listing, and retrieval.
 */
@RestController
@RequestMapping("/custom_form/domain")
public class DomainController {

    @Autowired
    DomainRepository domainRepository;

    @Autowired
    UserRepository userRepository;

    /**
     * Create a new domain. Only authenticated users can create domains.
     * The creating user becomes the domain owner (Business Owner role).
     */
    @PostMapping("")
    @PreAuthorize("hasAnyAuthority('BUSINESS_OWNER','DOMAIN_ADMIN','APP_ADMIN','BUSINESS_USER','ROLE_BUSINESS_OWNER','ROLE_DOMAIN_ADMIN','ROLE_APP_ADMIN','ROLE_BUSINESS_USER')")
    public ResponseEntity<?> createDomain(@Valid @RequestBody CreateDomainRequest createDomainRequest) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String currentUserId = userDetails.getId();

        // Normalize slug
        String normalizedSlug = slugify(createDomainRequest.getSlug());

        // Check if slug already exists
        if (domainRepository.existsBySlug(normalizedSlug)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new MessageResponse("Error: Preferred domain name (slug) is already taken!"));
        }

        // Verify user exists
        Optional<User> userOpt = userRepository.findById(currentUserId);
        if (!userOpt.isPresent()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: User not found!"));
        }

        // Create new domain
        Domain domain = new Domain(createDomainRequest.getName(), normalizedSlug, currentUserId);
        domain.setDescription(createDomainRequest.getDescription());
        domain.setIndustry(createDomainRequest.getIndustry());
        Domain savedDomain = domainRepository.save(domain);

        return ResponseEntity.ok(new DomainResponse(savedDomain));
    }

    /**
     * Get all domains owned by the current user.
     */
    @GetMapping("")
    @PreAuthorize("hasAnyAuthority('BUSINESS_USER', 'DOMAIN_ADMIN', 'BUSINESS_OWNER', 'APP_ADMIN', 'ROLE_BUSINESS_USER', 'ROLE_DOMAIN_ADMIN', 'ROLE_BUSINESS_OWNER', 'ROLE_APP_ADMIN')")
    public ResponseEntity<List<DomainResponse>> getUserDomains() {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String currentUserId = userDetails.getId();

        // Find domains owned by current user
        List<Domain> domains = domainRepository.findByOwnerUserId(currentUserId);
        List<DomainResponse> domainResponses = domains.stream()
                .map(DomainResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(domainResponses);
    }

    /**
     * Get specific domain by ID. Only the owner can access their domains.
     */
    @GetMapping("/{id}")
    @PreAuthorize("@domainSecurity.isSameDomain(#id, principal.domainId)")
    public ResponseEntity<?> getDomain(@PathVariable String id) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String currentUserId = userDetails.getId();

        // Find domain by ID
        Optional<Domain> domainOpt = domainRepository.findById(id);
        if (!domainOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Domain domain = domainOpt.get();

        // Check if current user is the owner of this domain
        if (!domain.getOwnerUserId().equals(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("Error: Access denied. You are not the owner of this domain."));
        }

        return ResponseEntity.ok(new DomainResponse(domain));
    }

    /**
     * Get specific domain by slug. Only the owner can access their domains.
     */
    @GetMapping("/slug/{slug}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getDomainBySlug(@PathVariable String slug) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String currentUserId = userDetails.getId();

        String normalizedSlug = slugify(slug);

        Optional<Domain> domainOpt = domainRepository.findBySlug(normalizedSlug);
        if (domainOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Domain domain = domainOpt.get();
        if (!domain.getOwnerUserId().equals(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("Error: Access denied. You are not the owner of this domain."));
        }

        return ResponseEntity.ok(new DomainResponse(domain));
    }

    // --- helpers ---
    private String slugify(String input) {
        if (input == null) return null;
        String s = input.trim().toLowerCase();
        // Replace non-alphanumeric with hyphen
        s = s.replaceAll("[^a-z0-9]+", "-");
        // Trim hyphens from ends
        s = s.replaceAll("^-+", "").replaceAll("-+$", "");
        return s;
    }
}