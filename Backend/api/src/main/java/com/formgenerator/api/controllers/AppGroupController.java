package com.formgenerator.api.controllers;

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

import com.formgenerator.api.dto.app.AppGroupMemberResponse;
import com.formgenerator.api.dto.app.AppUserResponse;
import com.formgenerator.api.dto.rbac.AssignMemberRequest;
import com.formgenerator.api.dto.rbac.CreateAppGroupRequest;
import com.formgenerator.api.models.app.AppGroup;
import com.formgenerator.api.models.app.AppGroupMember;
import com.formgenerator.api.models.app.Application;
import com.formgenerator.api.permissions.AppPermission;
import com.formgenerator.api.permissions.DomainPermission;
import com.formgenerator.api.repository.AppGroupMemberRepository;
import com.formgenerator.api.repository.AppGroupRepository;
import com.formgenerator.api.repository.ApplicationRepository;
import com.formgenerator.api.repository.DomainRepository;
import com.formgenerator.api.repository.DomainUserRepository;
import com.formgenerator.api.services.PermissionService;
import com.formgenerator.platform.auth.Domain;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/adaptive/domains/{slug}/apps/{appSlug}/groups")
public class AppGroupController {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private AppGroupRepository appGroupRepository;

    @Autowired
    private AppGroupMemberRepository appGroupMemberRepository;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private DomainUserRepository domainUserRepository;

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String slug, @PathVariable String appSlug) {
        ApplicationWithDomain appWithDomain = requireApplication(slug, appSlug);
        if (!permissionService.hasDomainPermission(appWithDomain.domain.getId(), DomainPermission.DOMAIN_MANAGE_APPS)) {
            return ResponseEntity.status(403).build();
        }
        List<AppGroup> groups = appGroupRepository.findByAppId(appWithDomain.application.getId());
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsersWithGroups(@PathVariable String slug, @PathVariable String appSlug) {
        ApplicationWithDomain appWithDomain = requireApplication(slug, appSlug);
        if (!permissionService.hasDomainPermission(appWithDomain.domain.getId(), DomainPermission.DOMAIN_MANAGE_APPS)) {
            return ResponseEntity.status(403).build();
        }
        
        // Get all users in the domain
        List<com.formgenerator.api.models.domain.DomainUser> users = domainUserRepository.findByDomainId(appWithDomain.domain.getId());
        
        // Get all app groups for mapping
        List<AppGroup> groups = appGroupRepository.findByAppId(appWithDomain.application.getId());
        Map<String, String> groupIdToName = groups.stream()
                .collect(Collectors.toMap(AppGroup::getId, AppGroup::getName));
        
        // Find the default viewer group
        AppGroup defaultViewerGroup = groups.stream()
                .filter(g -> g.isDefaultGroup() && "App Viewer".equalsIgnoreCase(g.getName()))
                .findFirst()
                .orElse(null);
        
        // Build response with app group memberships
        List<AppUserResponse> userResponses = users.stream().map(user -> {
            AppUserResponse response = new AppUserResponse(user);
            
            // Get user's app group memberships
            List<AppGroupMember> memberships = appGroupMemberRepository
                    .findByAppIdAndUserId(appWithDomain.application.getId(), user.getId());
            
            // If user has no groups and default viewer group exists, assign them to it
            if (memberships.isEmpty() && defaultViewerGroup != null) {
                AppGroupMember viewerMembership = new AppGroupMember();
                viewerMembership.setGroupId(defaultViewerGroup.getId());
                viewerMembership.setAppId(appWithDomain.application.getId());
                viewerMembership.setUserId(user.getId());
                viewerMembership.setAssignedBy("system");
                appGroupMemberRepository.save(viewerMembership);
                memberships = List.of(viewerMembership);
            }
            
            List<AppUserResponse.AppGroupMembershipInfo> groupInfos = memberships.stream()
                    .map(membership -> {
                        String groupName = groupIdToName.getOrDefault(membership.getGroupId(), "Unknown");
                        return new AppUserResponse.AppGroupMembershipInfo(
                                membership.getGroupId(),
                                groupName,
                                membership.getAssignedAt());
                    })
                    .collect(Collectors.toList());
            
            response.setAppGroups(groupInfos);
            return response;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(userResponses);
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<?> listGroupMembers(@PathVariable String slug, @PathVariable String appSlug,
            @PathVariable String groupId) {
        ApplicationWithDomain appWithDomain = requireApplication(slug, appSlug);
        AppGroup group = appGroupRepository.findById(groupId).orElse(null);
        if (group == null || !group.getAppId().equals(appWithDomain.application.getId())) {
            return ResponseEntity.notFound().build();
        }
        if (!permissionService.hasDomainPermission(appWithDomain.domain.getId(), DomainPermission.DOMAIN_MANAGE_APPS)) {
            return ResponseEntity.status(403).build();
        }
        
        // Get all members of this group
        List<AppGroupMember> memberships = appGroupMemberRepository.findByGroupId(groupId);
        
        // Fetch user details for each member
        List<AppGroupMemberResponse> memberResponses = memberships.stream()
                .map(membership -> {
                    return domainUserRepository.findById(membership.getUserId())
                            .map(user -> new AppGroupMemberResponse(
                                    user.getId(),
                                    user.getUsername(),
                                    user.getEmail(),
                                    user.getStatus(),
                                    membership.getAssignedAt(),
                                    membership.getAssignedBy()))
                            .orElse(null);
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(memberResponses);
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable String slug, @PathVariable String appSlug,
            @Valid @RequestBody CreateAppGroupRequest request) {
        ApplicationWithDomain appWithDomain = requireApplication(slug, appSlug);
        if (!permissionService.hasDomainPermission(appWithDomain.domain.getId(), DomainPermission.DOMAIN_MANAGE_APPS)) {
            return ResponseEntity.status(403).build();
        }
        boolean exists = appGroupRepository.findByAppIdAndName(appWithDomain.application.getId(), request.getName())
                .isPresent();
        if (exists) {
            return ResponseEntity.badRequest().body("Group already exists");
        }
        AppGroup group = new AppGroup();
        group.setAppId(appWithDomain.application.getId());
        group.setName(request.getName());
        group.setPermissions(request.getPermissions());
        group.setDefaultGroup(false);
        return ResponseEntity.ok(appGroupRepository.save(group));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addMember(@PathVariable String slug, @PathVariable String appSlug,
            @PathVariable String groupId, @Valid @RequestBody AssignMemberRequest request) {
        ApplicationWithDomain appWithDomain = requireApplication(slug, appSlug);
        if (!permissionService.hasDomainPermission(appWithDomain.domain.getId(), DomainPermission.DOMAIN_MANAGE_APPS)) {
            return ResponseEntity.status(403).build();
        }
        AppGroup group = appGroupRepository.findById(groupId).orElse(null);
        if (group == null || !group.getAppId().equals(appWithDomain.application.getId())) {
            return ResponseEntity.notFound().build();
        }
        var userOpt = domainUserRepository.findByDomainIdAndUsername(appWithDomain.domain.getId(), request.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found in domain");
        }
        String userId = userOpt.get().getId();
        boolean exists = appGroupMemberRepository.findByGroupIdAndUserId(groupId, userId).isPresent();
        if (exists) {
            return ResponseEntity.badRequest().body("User already in group");
        }
        AppGroupMember member = new AppGroupMember();
        member.setGroupId(groupId);
        member.setAppId(appWithDomain.application.getId());
        member.setUserId(userId);
        member.setAssignedBy(currentPrincipalId());
        appGroupMemberRepository.save(member);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<?> listUserGroups(@PathVariable String slug, @PathVariable String appSlug,
            @PathVariable String userId) {
        ApplicationWithDomain appWithDomain = requireApplication(slug, appSlug);
        if (!permissionService.hasDomainPermission(appWithDomain.domain.getId(), DomainPermission.DOMAIN_MANAGE_APPS)) {
            return ResponseEntity.status(403).build();
        }
        
        // Verify user exists in this domain
        var userOpt = domainUserRepository.findById(userId);
        if (userOpt.isEmpty() || !userOpt.get().getDomainId().equals(appWithDomain.domain.getId())) {
            return ResponseEntity.notFound().build();
        }
        
        // Get user's app group memberships
        List<AppGroupMember> memberships = appGroupMemberRepository
                .findByAppIdAndUserId(appWithDomain.application.getId(), userId);
        
        // Fetch group details
        List<AppGroup> userGroups = memberships.stream()
                .map(membership -> appGroupRepository.findById(membership.getGroupId()).orElse(null))
                .filter(group -> group != null)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(userGroups);
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable String slug, @PathVariable String appSlug,
            @PathVariable String groupId, @PathVariable String userId) {
        ApplicationWithDomain appWithDomain = requireApplication(slug, appSlug);
        if (!permissionService.hasDomainPermission(appWithDomain.domain.getId(), DomainPermission.DOMAIN_MANAGE_APPS)) {
            return ResponseEntity.status(403).build();
        }
        AppGroup group = appGroupRepository.findById(groupId).orElse(null);
        if (group == null || !group.getAppId().equals(appWithDomain.application.getId())) {
            return ResponseEntity.notFound().build();
        }
        
        // Remove the user from the group
        appGroupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .ifPresent(appGroupMemberRepository::delete);
        
        // Check if user still has any groups after removal
        List<AppGroupMember> remainingMemberships = appGroupMemberRepository
                .findByAppIdAndUserId(appWithDomain.application.getId(), userId);
        
        // If user has no groups left, add them to the default "App Viewer" group
        if (remainingMemberships.isEmpty()) {
            List<AppGroup> groups = appGroupRepository.findByAppId(appWithDomain.application.getId());
            AppGroup defaultViewerGroup = groups.stream()
                    .filter(g -> g.isDefaultGroup() && "App Viewer".equalsIgnoreCase(g.getName()))
                    .findFirst()
                    .orElse(null);
            
            if (defaultViewerGroup != null) {
                AppGroupMember viewerMembership = new AppGroupMember();
                viewerMembership.setGroupId(defaultViewerGroup.getId());
                viewerMembership.setAppId(appWithDomain.application.getId());
                viewerMembership.setUserId(userId);
                viewerMembership.setAssignedBy("system");
                appGroupMemberRepository.save(viewerMembership);
            }
        }
        
        return ResponseEntity.ok().build();
    }

    private ApplicationWithDomain requireApplication(String slug, String appSlug) {
        Domain domain = domainRepository.findBySlug(slugify(slug))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found"));
        var application = applicationRepository.findByDomainIdAndSlug(domain.getId(), appSlug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        return new ApplicationWithDomain(domain, application);
    }

    private record ApplicationWithDomain(Domain domain, Application application) {
    }

    private String slugify(String input) {
        if (input == null)
            return null;
        String s = input.trim().toLowerCase();
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+", "").replaceAll("-+$", "");
        return s;
    }

    private String currentPrincipalId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof com.formgenerator.platform.auth.AdaptiveUserDetails details) {
            return details.getId();
        }
        return null;
    }
}
