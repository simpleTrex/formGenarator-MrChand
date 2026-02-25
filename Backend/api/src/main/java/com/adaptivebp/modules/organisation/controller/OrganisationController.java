package com.adaptivebp.modules.organisation.controller;

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

import com.adaptivebp.modules.identity.dto.response.MessageResponse;
import com.adaptivebp.modules.organisation.dto.request.CreateOrganisationRequest;
import com.adaptivebp.modules.organisation.dto.response.OrganisationResponse;
import com.adaptivebp.modules.organisation.model.Organisation;
import com.adaptivebp.modules.organisation.repository.OrganisationRepository;
import com.adaptivebp.modules.organisation.service.DomainProvisioningService;
import com.adaptivebp.shared.security.AdaptiveUserDetails;
import com.adaptivebp.shared.security.PrincipalType;
import com.adaptivebp.shared.security.UserDetailsImpl;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/custom_form/domain")
public class OrganisationController {

    @Autowired
    OrganisationRepository organisationRepository;

    @Autowired
    DomainProvisioningService domainProvisioningService;

    @PostMapping("")
    @PreAuthorize("@permissionService.isOwner()")
    public ResponseEntity<?> createDomain(@Valid @RequestBody CreateOrganisationRequest request) {
        AdaptiveUserDetails owner = currentAdaptivePrincipal();
        if (owner == null || owner.getPrincipalType() != PrincipalType.OWNER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String currentUserId = owner.getId();

        String normalizedSlug = slugify(request.getSlug());
        if (organisationRepository.existsBySlug(normalizedSlug)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new MessageResponse("Error: Preferred domain name (slug) is already taken!"));
        }

        Organisation organisation = new Organisation(request.getName(), normalizedSlug, currentUserId);
        organisation.setDescription(request.getDescription());
        organisation.setIndustry(request.getIndustry());
        Organisation saved = organisationRepository.save(organisation);
        domainProvisioningService.provisionDefaults(saved, null);

        return ResponseEntity.ok(new OrganisationResponse(saved));
    }

    @GetMapping("")
    public ResponseEntity<List<OrganisationResponse>> getUserDomains() {
        AdaptiveUserDetails principal = currentAdaptivePrincipal();
        if (principal != null) {
            if (principal.getPrincipalType() == PrincipalType.OWNER) {
                List<Organisation> orgs = organisationRepository.findByOwnerUserId(principal.getId());
                return ResponseEntity.ok(orgs.stream().map(OrganisationResponse::new).collect(Collectors.toList()));
            }
            if (principal.getPrincipalType() == PrincipalType.DOMAIN_USER && principal.getDomainId() != null) {
                return organisationRepository.findById(principal.getDomainId())
                        .map(org -> ResponseEntity.ok(List.of(new OrganisationResponse(org))))
                        .orElse(ResponseEntity.ok(List.of()));
            }
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            String currentUserId = userDetails.getId();
            List<Organisation> orgs = organisationRepository.findByOwnerUserId(currentUserId);
            return ResponseEntity.ok(orgs.stream().map(OrganisationResponse::new).collect(Collectors.toList()));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDomain(@PathVariable String id) {
        Optional<Organisation> orgOpt = organisationRepository.findById(id);
        if (orgOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Organisation org = orgOpt.get();

        AdaptiveUserDetails principal = currentAdaptivePrincipal();
        if (principal != null) {
            if (principal.getPrincipalType() == PrincipalType.OWNER
                    && org.getOwnerUserId().equals(principal.getId())) {
                return ResponseEntity.ok(new OrganisationResponse(org));
            }
            if (principal.getPrincipalType() == PrincipalType.DOMAIN_USER
                    && id.equals(principal.getDomainId())) {
                return ResponseEntity.ok(new OrganisationResponse(org));
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("Error: Access denied."));
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            String currentUserId = userDetails.getId();
            if (!org.getOwnerUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new MessageResponse("Error: Access denied. You are not the owner of this domain."));
            }
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(new OrganisationResponse(org));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<?> getDomainBySlug(@PathVariable String slug) {
        String normalizedSlug = slugify(slug);
        Optional<Organisation> orgOpt = organisationRepository.findBySlug(normalizedSlug);
        if (orgOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Organisation org = orgOpt.get();
        AdaptiveUserDetails principal = currentAdaptivePrincipal();
        if (principal != null) {
            if (principal.getPrincipalType() == PrincipalType.OWNER
                    && org.getOwnerUserId().equals(principal.getId())) {
                return ResponseEntity.ok(new OrganisationResponse(org));
            }
            if (principal.getPrincipalType() == PrincipalType.DOMAIN_USER
                    && org.getId().equals(principal.getDomainId())) {
                return ResponseEntity.ok(new OrganisationResponse(org));
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("Error: Access denied."));
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            String currentUserId = userDetails.getId();
            if (!org.getOwnerUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new MessageResponse("Error: Access denied. You are not the owner of this domain."));
            }
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(new OrganisationResponse(org));
    }

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
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+", "").replaceAll("-+$", "");
        return s;
    }
}
