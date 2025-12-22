package com.formgenerator.api.controllers;

import java.util.List;
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
import com.formgenerator.api.services.DomainProvisioningService;
import com.formgenerator.api.services.PermissionService;
import com.formgenerator.platform.auth.AdaptiveUserDetails;
import com.formgenerator.platform.auth.CreateDomainRequest;
import com.formgenerator.platform.auth.Domain;
import com.formgenerator.platform.auth.DomainResponse;
import com.formgenerator.platform.auth.MessageResponse;
import com.formgenerator.platform.auth.PrincipalType;
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
    DomainProvisioningService domainProvisioningService;

    /**
     * Create a new domain. Only authenticated users can create domains.
     * The creating user becomes the domain owner (Business Owner role).
     */
    @PostMapping("")
    @PreAuthorize("@permissionService.isOwner()")
    public ResponseEntity<?> createDomain(@Valid @RequestBody CreateDomainRequest createDomainRequest) {
        AdaptiveUserDetails owner = currentAdaptivePrincipal();
        if (owner == null || owner.getPrincipalType() != PrincipalType.OWNER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String currentUserId = owner.getId();

        // Normalize slug
        String normalizedSlug = slugify(createDomainRequest.getSlug());

        // Check if slug already exists
        if (domainRepository.existsBySlug(normalizedSlug)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new MessageResponse("Error: Preferred domain name (slug) is already taken!"));
        }

        // Create new domain
        Domain domain = new Domain(createDomainRequest.getName(), normalizedSlug, currentUserId);
        domain.setDescription(createDomainRequest.getDescription());
        domain.setIndustry(createDomainRequest.getIndustry());
        Domain savedDomain = domainRepository.save(domain);
        domainProvisioningService.provisionDefaults(savedDomain, null);

        return ResponseEntity.ok(new DomainResponse(savedDomain));
    }

    /**
     * Get all domains owned by the current user.
     */
    @GetMapping("")
    public ResponseEntity<List<DomainResponse>> getUserDomains() {
        AdaptiveUserDetails principal = currentAdaptivePrincipal();
        if (principal != null) {
            if (principal.getPrincipalType() == PrincipalType.OWNER) {
                List<Domain> domains = domainRepository.findByOwnerUserId(principal.getId());
                return ResponseEntity.ok(domains.stream().map(DomainResponse::new).collect(Collectors.toList()));
            }
            if (principal.getPrincipalType() == PrincipalType.DOMAIN_USER && principal.getDomainId() != null) {
                return domainRepository.findById(principal.getDomainId())
                        .map(domain -> ResponseEntity.ok(List.of(new DomainResponse(domain))))
                        .orElse(ResponseEntity.ok(List.of()));
            }
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            String currentUserId = userDetails.getId();
            List<Domain> domains = domainRepository.findByOwnerUserId(currentUserId);
            List<DomainResponse> domainResponses = domains.stream()
                    .map(DomainResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(domainResponses);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * Get specific domain by ID. Only the owner can access their domains.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDomain(@PathVariable String id) {
        // Find domain by ID
        Optional<Domain> domainOpt = domainRepository.findById(id);
        if (!domainOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Domain domain = domainOpt.get();

        AdaptiveUserDetails principal = currentAdaptivePrincipal();
        if (principal != null) {
            if (principal.getPrincipalType() == PrincipalType.OWNER
                    && domain.getOwnerUserId().equals(principal.getId())) {
                return ResponseEntity.ok(new DomainResponse(domain));
            }
            if (principal.getPrincipalType() == PrincipalType.DOMAIN_USER
                    && id.equals(principal.getDomainId())) {
                return ResponseEntity.ok(new DomainResponse(domain));
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("Error: Access denied."));
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            String currentUserId = userDetails.getId();
            if (!domain.getOwnerUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new MessageResponse("Error: Access denied. You are not the owner of this domain."));
            }
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(new DomainResponse(domain));
    }

    /**
     * Get specific domain by slug. Only the owner can access their domains.
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<?> getDomainBySlug(@PathVariable String slug) {
        String normalizedSlug = slugify(slug);

        Optional<Domain> domainOpt = domainRepository.findBySlug(normalizedSlug);
        if (domainOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Domain domain = domainOpt.get();
        AdaptiveUserDetails principal = currentAdaptivePrincipal();
        if (principal != null) {
            if (principal.getPrincipalType() == PrincipalType.OWNER
                    && domain.getOwnerUserId().equals(principal.getId())) {
                return ResponseEntity.ok(new DomainResponse(domain));
            }
            if (principal.getPrincipalType() == PrincipalType.DOMAIN_USER
                    && domain.getId().equals(principal.getDomainId())) {
                return ResponseEntity.ok(new DomainResponse(domain));
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("Error: Access denied."));
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            String currentUserId = userDetails.getId();
            if (!domain.getOwnerUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new MessageResponse("Error: Access denied. You are not the owner of this domain."));
            }
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(new DomainResponse(domain));
    }

    // --- helpers ---
    private AdaptiveUserDetails currentAdaptivePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AdaptiveUserDetails details) {
            return details;
        }
        return null;
    }

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
