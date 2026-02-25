package com.adaptivebp.modules.organisation.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.adaptivebp.modules.identity.model.DomainUser;
import com.adaptivebp.modules.identity.repository.DomainUserRepository;
import com.adaptivebp.modules.organisation.dto.request.AssignMemberRequest;
import com.adaptivebp.modules.organisation.dto.request.CreateDomainGroupRequest;
import com.adaptivebp.modules.organisation.dto.response.DomainUserResponse;
import com.adaptivebp.modules.organisation.dto.response.GroupMemberResponse;
import com.adaptivebp.modules.organisation.model.DomainGroup;
import com.adaptivebp.modules.organisation.model.DomainGroupMember;
import com.adaptivebp.modules.organisation.model.Organisation;
import com.adaptivebp.modules.organisation.permission.DomainPermission;
import com.adaptivebp.modules.organisation.repository.DomainGroupMemberRepository;
import com.adaptivebp.modules.organisation.repository.DomainGroupRepository;
import com.adaptivebp.modules.organisation.repository.OrganisationRepository;
import com.adaptivebp.modules.organisation.service.PermissionService;
import com.adaptivebp.shared.security.AdaptiveUserDetails;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/adaptive/domains/{slug}/groups")
public class DomainGroupController {

    @Autowired
    private OrganisationRepository organisationRepository;
    @Autowired
    private DomainGroupRepository domainGroupRepository;
    @Autowired
    private DomainGroupMemberRepository domainGroupMemberRepository;
    @Autowired
    private DomainUserRepository domainUserRepository;
    @Autowired
    private PermissionService permissionService;

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String slug) {
        Organisation domain = requireDomain(slug);
        if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_MANAGE_USERS)) {
            return ResponseEntity.status(403).build();
        }
        List<DomainGroup> groups = domainGroupRepository.findByDomainId(domain.getId());
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsersWithGroups(@PathVariable String slug) {
        Organisation domain = requireDomain(slug);
        if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_MANAGE_USERS)) {
            return ResponseEntity.status(403).build();
        }
        List<DomainUser> users = domainUserRepository.findByDomainId(domain.getId());
        List<DomainGroup> groups = domainGroupRepository.findByDomainId(domain.getId());
        Map<String, String> groupIdToName = groups.stream()
                .collect(Collectors.toMap(DomainGroup::getId, DomainGroup::getName));
        List<DomainUserResponse> userResponses = users.stream().map(user -> {
            DomainUserResponse response = new DomainUserResponse(user);
            List<DomainGroupMember> memberships = domainGroupMemberRepository
                    .findByDomainIdAndUserId(domain.getId(), user.getId());
            List<DomainUserResponse.GroupMembershipInfo> groupInfos = memberships.stream()
                    .map(m -> new DomainUserResponse.GroupMembershipInfo(
                            m.getDomainGroupId(),
                            groupIdToName.getOrDefault(m.getDomainGroupId(), "Unknown"),
                            m.getAssignedAt()))
                    .collect(Collectors.toList());
            response.setGroups(groupInfos);
            return response;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(userResponses);
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<?> listGroupMembers(@PathVariable String slug, @PathVariable String groupId) {
        Organisation domain = requireDomain(slug);
        DomainGroup group = domainGroupRepository.findById(groupId).orElse(null);
        if (group == null || !domain.getId().equals(group.getDomainId())) {
            return ResponseEntity.notFound().build();
        }
        if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_MANAGE_USERS)) {
            return ResponseEntity.status(403).build();
        }
        List<DomainGroupMember> memberships = domainGroupMemberRepository.findByDomainGroupId(groupId);
        List<GroupMemberResponse> memberResponses = memberships.stream()
                .map(membership -> domainUserRepository.findById(membership.getUserId())
                        .map(user -> new GroupMemberResponse(user.getId(), user.getUsername(), user.getEmail(),
                                user.getStatus(), membership.getAssignedAt(), membership.getAssignedBy()))
                        .orElse(null))
                .filter(r -> r != null)
                .collect(Collectors.toList());
        return ResponseEntity.ok(memberResponses);
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable String slug, @Valid @RequestBody CreateDomainGroupRequest request) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Domain groups are managed automatically and cannot be created manually.");
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addMember(@PathVariable String slug, @PathVariable String groupId,
            @Valid @RequestBody AssignMemberRequest request) {
        Organisation domain = requireDomain(slug);
        DomainGroup group = domainGroupRepository.findById(groupId).orElse(null);
        if (group == null || !domain.getId().equals(group.getDomainId())) {
            return ResponseEntity.notFound().build();
        }
        if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_MANAGE_USERS)) {
            return ResponseEntity.status(403).build();
        }
        var userOpt = domainUserRepository.findByDomainIdAndUsername(domain.getId(), request.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found in this domain");
        }
        String userId = userOpt.get().getId();
        boolean exists = domainGroupMemberRepository.findByDomainGroupIdAndUserId(groupId, userId).isPresent();
        if (exists) {
            return ResponseEntity.badRequest().body("User already in group");
        }
        DomainGroupMember member = new DomainGroupMember();
        member.setDomainGroupId(groupId);
        member.setDomainId(domain.getId());
        member.setUserId(userId);
        member.setAssignedBy(currentPrincipalId());
        domainGroupMemberRepository.save(member);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<?> listUserGroups(@PathVariable String slug, @PathVariable String userId) {
        Organisation domain = requireDomain(slug);
        if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_MANAGE_USERS)) {
            return ResponseEntity.status(403).build();
        }
        var userOpt = domainUserRepository.findById(userId);
        if (userOpt.isEmpty() || !userOpt.get().getDomainId().equals(domain.getId())) {
            return ResponseEntity.notFound().build();
        }
        List<DomainGroupMember> memberships = domainGroupMemberRepository
                .findByDomainIdAndUserId(domain.getId(), userId);
        List<DomainGroup> userGroups = memberships.stream()
                .map(m -> domainGroupRepository.findById(m.getDomainGroupId()).orElse(null))
                .filter(g -> g != null)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userGroups);
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable String slug, @PathVariable String groupId,
            @PathVariable String userId) {
        Organisation domain = requireDomain(slug);
        DomainGroup group = domainGroupRepository.findById(groupId).orElse(null);
        if (group == null || !domain.getId().equals(group.getDomainId())) {
            return ResponseEntity.notFound().build();
        }
        if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_MANAGE_USERS)) {
            return ResponseEntity.status(403).build();
        }
        domainGroupMemberRepository.findByDomainGroupIdAndUserId(groupId, userId)
                .ifPresent(domainGroupMemberRepository::delete);
        return ResponseEntity.ok().build();
    }

    private Organisation requireDomain(String slug) {
        return organisationRepository.findBySlug(slugify(slug))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found"));
    }

    private String slugify(String input) {
        if (input == null) return null;
        String s = input.trim().toLowerCase();
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+", "").replaceAll("-+$", "");
        return s;
    }

    private String currentPrincipalId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AdaptiveUserDetails details) {
            return details.getId();
        }
        return null;
    }
}
