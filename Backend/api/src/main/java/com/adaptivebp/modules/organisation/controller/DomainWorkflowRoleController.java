package com.adaptivebp.modules.organisation.controller;

import java.util.EnumSet;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.adaptivebp.modules.identity.model.DomainUser;
import com.adaptivebp.modules.identity.port.DomainUserLookupPort;
import com.adaptivebp.modules.organisation.dto.request.AssignMemberRequest;
import com.adaptivebp.modules.organisation.dto.request.CreateWorkflowRoleRequest;
import com.adaptivebp.modules.organisation.dto.response.DomainUserResponse;
import com.adaptivebp.modules.organisation.model.DomainGroup;
import com.adaptivebp.modules.organisation.model.DomainGroupMember;
import com.adaptivebp.modules.organisation.model.Organisation;
import com.adaptivebp.modules.organisation.model.enums.DomainGroupType;
import com.adaptivebp.modules.organisation.permission.DomainPermission;
import com.adaptivebp.modules.organisation.repository.DomainGroupMemberRepository;
import com.adaptivebp.modules.organisation.repository.DomainGroupRepository;
import com.adaptivebp.modules.organisation.repository.OrganisationRepository;
import com.adaptivebp.modules.organisation.service.PermissionService;
import com.adaptivebp.shared.security.AdaptiveUserDetails;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/adaptive/domains/{slug}/workflow-roles")
public class DomainWorkflowRoleController {

    @Autowired
    private OrganisationRepository organisationRepository;
    @Autowired
    private DomainGroupRepository domainGroupRepository;
    @Autowired
    private DomainGroupMemberRepository domainGroupMemberRepository;
    @Autowired
    private DomainUserLookupPort domainUserLookupPort;
    @Autowired
    private PermissionService permissionService;

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String slug) {
        Organisation domain = requireDomain(slug);
        if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_USE_APP)
                && !permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_MANAGE_USERS)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(workflowRoles(domain.getId()));
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsersWithRoles(@PathVariable String slug) {
        Organisation domain = requireDomain(slug);
        if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_MANAGE_USERS)) {
            return ResponseEntity.status(403).build();
        }

        List<DomainUser> users = domainUserLookupPort.findByDomainId(domain.getId());
        List<DomainGroup> roles = workflowRoles(domain.getId());
        Map<String, String> roleIdToName = roles.stream()
                .collect(Collectors.toMap(DomainGroup::getId, DomainGroup::getName));

        List<DomainUserResponse> userResponses = users.stream().map(user -> {
            DomainUserResponse response = new DomainUserResponse(user);
            List<DomainGroupMember> memberships = domainGroupMemberRepository
                    .findByDomainIdAndUserId(domain.getId(), user.getId());
            List<DomainUserResponse.GroupMembershipInfo> roleInfos = memberships.stream()
                    .filter(m -> roleIdToName.containsKey(m.getDomainGroupId()))
                    .map(m -> new DomainUserResponse.GroupMembershipInfo(
                            m.getDomainGroupId(),
                            roleIdToName.get(m.getDomainGroupId()),
                            m.getAssignedAt()))
                    .collect(Collectors.toList());
            response.setGroups(roleInfos);
            return response;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(userResponses);
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable String slug, @Valid @RequestBody CreateWorkflowRoleRequest request) {
        Organisation domain = requireDomain(slug);
        if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_MANAGE_USERS)) {
            return ResponseEntity.status(403).build();
        }

        String normalizedName = request.getName() != null ? request.getName().trim() : null;
        boolean exists = domainGroupRepository.findByDomainIdAndName(domain.getId(), normalizedName).isPresent();
        if (exists) {
            return ResponseEntity.badRequest().body("Role already exists");
        }

        DomainGroup role = new DomainGroup();
        role.setDomainId(domain.getId());
        role.setName(normalizedName);
        role.setPermissions(EnumSet.noneOf(DomainPermission.class));
        role.setGroupType(DomainGroupType.WORKFLOW_ROLE);
        role.setDefaultGroup(false);
        return ResponseEntity.status(HttpStatus.CREATED).body(domainGroupRepository.save(role));
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<?> update(@PathVariable String slug, @PathVariable String roleId,
            @Valid @RequestBody CreateWorkflowRoleRequest request) {
        Organisation domain = requireDomain(slug);
        if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_MANAGE_USERS)) {
            return ResponseEntity.status(403).build();
        }

        DomainGroup role = domainGroupRepository.findById(roleId).orElse(null);
        if (role == null || !domain.getId().equals(role.getDomainId()) || !isWorkflowRole(role)) {
            return ResponseEntity.notFound().build();
        }

        String normalizedName = request.getName() != null ? request.getName().trim() : null;
        boolean duplicateName = domainGroupRepository.findByDomainIdAndName(domain.getId(), normalizedName)
                .filter(existing -> !existing.getId().equals(role.getId()))
                .isPresent();
        if (duplicateName) {
            return ResponseEntity.badRequest().body("Role already exists");
        }

        role.setName(normalizedName);
        return ResponseEntity.ok(domainGroupRepository.save(role));
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<?> delete(@PathVariable String slug, @PathVariable String roleId) {
        Organisation domain = requireDomain(slug);
        if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_MANAGE_USERS)) {
            return ResponseEntity.status(403).build();
        }

        DomainGroup role = domainGroupRepository.findById(roleId).orElse(null);
        if (role == null || !domain.getId().equals(role.getDomainId()) || !isWorkflowRole(role)) {
            return ResponseEntity.notFound().build();
        }

        List<DomainGroupMember> members = domainGroupMemberRepository.findByDomainGroupId(roleId);
        for (DomainGroupMember member : members) {
            domainGroupMemberRepository.delete(member);
        }
        domainGroupRepository.delete(role);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{roleId}/members")
    public ResponseEntity<?> addMember(@PathVariable String slug, @PathVariable String roleId,
            @Valid @RequestBody AssignMemberRequest request) {
        Organisation domain = requireDomain(slug);
        if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_MANAGE_USERS)) {
            return ResponseEntity.status(403).build();
        }

        DomainGroup role = domainGroupRepository.findById(roleId).orElse(null);
        if (role == null || !domain.getId().equals(role.getDomainId()) || !isWorkflowRole(role)) {
            return ResponseEntity.notFound().build();
        }

        var userOpt = domainUserLookupPort.findByDomainIdAndUsername(domain.getId(), request.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found in this domain");
        }

        String userId = userOpt.get().getId();
        boolean exists = domainGroupMemberRepository.findByDomainGroupIdAndUserId(roleId, userId).isPresent();
        if (exists) {
            return ResponseEntity.badRequest().body("User already in role");
        }

        DomainGroupMember member = new DomainGroupMember();
        member.setDomainGroupId(roleId);
        member.setDomainId(domain.getId());
        member.setUserId(userId);
        member.setAssignedBy(currentPrincipalId());
        domainGroupMemberRepository.save(member);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{roleId}/members/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable String slug, @PathVariable String roleId,
            @PathVariable String userId) {
        Organisation domain = requireDomain(slug);
        if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_MANAGE_USERS)) {
            return ResponseEntity.status(403).build();
        }

        DomainGroup role = domainGroupRepository.findById(roleId).orElse(null);
        if (role == null || !domain.getId().equals(role.getDomainId()) || !isWorkflowRole(role)) {
            return ResponseEntity.notFound().build();
        }

        domainGroupMemberRepository.findByDomainGroupIdAndUserId(roleId, userId)
                .ifPresent(domainGroupMemberRepository::delete);
        return ResponseEntity.ok().build();
    }

    private List<DomainGroup> workflowRoles(String domainId) {
        return domainGroupRepository.findByDomainId(domainId).stream()
                .filter(this::isWorkflowRole)
                .collect(Collectors.toList());
    }

    private boolean isWorkflowRole(DomainGroup group) {
        return group.getGroupType() == DomainGroupType.WORKFLOW_ROLE;
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
