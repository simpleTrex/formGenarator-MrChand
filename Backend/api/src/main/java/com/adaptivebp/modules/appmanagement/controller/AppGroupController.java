package com.adaptivebp.modules.appmanagement.controller;

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

import com.adaptivebp.modules.appmanagement.dto.request.CreateAppGroupRequest;
import com.adaptivebp.modules.appmanagement.dto.response.AppGroupMemberResponse;
import com.adaptivebp.modules.appmanagement.dto.response.AppUserResponse;
import com.adaptivebp.modules.appmanagement.model.AppGroup;
import com.adaptivebp.modules.appmanagement.model.AppGroupMember;
import com.adaptivebp.modules.appmanagement.model.Application;
import com.adaptivebp.modules.appmanagement.permission.AppPermission;
import com.adaptivebp.modules.appmanagement.repository.AppGroupMemberRepository;
import com.adaptivebp.modules.appmanagement.repository.AppGroupRepository;
import com.adaptivebp.modules.appmanagement.repository.ApplicationRepository;
import com.adaptivebp.modules.identity.model.DomainUser;
import com.adaptivebp.modules.identity.repository.DomainUserRepository;
import com.adaptivebp.modules.organisation.dto.request.AssignMemberRequest;
import com.adaptivebp.modules.organisation.model.Organisation;
import com.adaptivebp.modules.organisation.permission.DomainPermission;
import com.adaptivebp.modules.organisation.repository.OrganisationRepository;
import com.adaptivebp.modules.organisation.service.PermissionService;
import com.adaptivebp.shared.security.AdaptiveUserDetails;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/adaptive/domains/{slug}/apps/{appSlug}/groups")
public class AppGroupController {

    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private AppGroupRepository appGroupRepository;
    @Autowired private AppGroupMemberRepository appGroupMemberRepository;
    @Autowired private PermissionService permissionService;
    @Autowired private OrganisationRepository organisationRepository;
    @Autowired private DomainUserRepository domainUserRepository;

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String slug, @PathVariable String appSlug) {
        AppWithDomain awd = requireApplication(slug, appSlug);
        if (!permissionService.hasAppPermission(awd.app.getId(), AppPermission.APP_READ) &&
            !permissionService.hasDomainPermission(awd.org.getId(), DomainPermission.DOMAIN_MANAGE_APPS)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(appGroupRepository.findByAppId(awd.app.getId()));
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsersWithGroups(@PathVariable String slug, @PathVariable String appSlug) {
        AppWithDomain awd = requireApplication(slug, appSlug);
        if (!permissionService.hasAppPermission(awd.app.getId(), AppPermission.APP_WRITE) &&
            !permissionService.hasDomainPermission(awd.org.getId(), DomainPermission.DOMAIN_MANAGE_APPS)) {
            return ResponseEntity.status(403).build();
        }
        List<DomainUser> users = domainUserRepository.findByDomainId(awd.org.getId());
        List<AppGroup> groups = appGroupRepository.findByAppId(awd.app.getId());
        Map<String, String> groupIdToName = groups.stream()
                .collect(Collectors.toMap(AppGroup::getId, AppGroup::getName));
        AppGroup defaultViewerGroup = groups.stream()
                .filter(g -> g.isDefaultGroup() && "App Viewer".equalsIgnoreCase(g.getName()))
                .findFirst().orElse(null);
        List<AppUserResponse> userResponses = users.stream().map(user -> {
            AppUserResponse response = new AppUserResponse(user);
            List<AppGroupMember> memberships = appGroupMemberRepository
                    .findByAppIdAndUserId(awd.app.getId(), user.getId());
            if (memberships.isEmpty() && defaultViewerGroup != null) {
                AppGroupMember viewerMembership = new AppGroupMember();
                viewerMembership.setGroupId(defaultViewerGroup.getId());
                viewerMembership.setAppId(awd.app.getId());
                viewerMembership.setUserId(user.getId());
                viewerMembership.setAssignedBy("system");
                appGroupMemberRepository.save(viewerMembership);
                memberships = List.of(viewerMembership);
            }
            List<AppUserResponse.AppGroupMembershipInfo> groupInfos = memberships.stream()
                    .map(m -> new AppUserResponse.AppGroupMembershipInfo(
                            m.getGroupId(),
                            groupIdToName.getOrDefault(m.getGroupId(), "Unknown"),
                            m.getAssignedAt()))
                    .collect(Collectors.toList());
            response.setAppGroups(groupInfos);
            return response;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(userResponses);
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<?> listGroupMembers(@PathVariable String slug, @PathVariable String appSlug,
            @PathVariable String groupId) {
        AppWithDomain awd = requireApplication(slug, appSlug);
        AppGroup group = appGroupRepository.findById(groupId).orElse(null);
        if (group == null || !group.getAppId().equals(awd.app.getId())) {
            return ResponseEntity.notFound().build();
        }
        if (!permissionService.hasAppPermission(awd.app.getId(), AppPermission.APP_READ) &&
            !permissionService.hasDomainPermission(awd.org.getId(), DomainPermission.DOMAIN_MANAGE_APPS)) {
            return ResponseEntity.status(403).build();
        }
        List<AppGroupMember> memberships = appGroupMemberRepository.findByGroupId(groupId);
        List<AppGroupMemberResponse> memberResponses = memberships.stream()
                .map(m -> domainUserRepository.findById(m.getUserId())
                        .map(user -> new AppGroupMemberResponse(user.getId(), user.getUsername(), user.getEmail(),
                                user.getStatus(), m.getAssignedAt(), m.getAssignedBy()))
                        .orElse(null))
                .filter(r -> r != null).collect(Collectors.toList());
        return ResponseEntity.ok(memberResponses);
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable String slug, @PathVariable String appSlug,
            @Valid @RequestBody CreateAppGroupRequest request) {
        AppWithDomain awd = requireApplication(slug, appSlug);
        if (!permissionService.hasDomainPermission(awd.org.getId(), DomainPermission.DOMAIN_MANAGE_APPS)) {
            return ResponseEntity.status(403).build();
        }
        boolean exists = appGroupRepository.findByAppIdAndName(awd.app.getId(), request.getName()).isPresent();
        if (exists) { return ResponseEntity.badRequest().body("Group already exists"); }
        AppGroup group = new AppGroup();
        group.setAppId(awd.app.getId());
        group.setName(request.getName());
        group.setPermissions(request.getPermissions());
        group.setDefaultGroup(false);
        return ResponseEntity.ok(appGroupRepository.save(group));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addMember(@PathVariable String slug, @PathVariable String appSlug,
            @PathVariable String groupId, @Valid @RequestBody AssignMemberRequest request) {
        AppWithDomain awd = requireApplication(slug, appSlug);
        if (!permissionService.hasAppPermission(awd.app.getId(), AppPermission.APP_WRITE) &&
            !permissionService.hasDomainPermission(awd.org.getId(), DomainPermission.DOMAIN_MANAGE_APPS)) {
            return ResponseEntity.status(403).build();
        }
        AppGroup group = appGroupRepository.findById(groupId).orElse(null);
        if (group == null || !group.getAppId().equals(awd.app.getId())) {
            return ResponseEntity.notFound().build();
        }
        var userOpt = domainUserRepository.findByDomainIdAndUsername(awd.org.getId(), request.getUsername());
        if (userOpt.isEmpty()) { return ResponseEntity.badRequest().body("User not found in domain"); }
        String userId = userOpt.get().getId();
        boolean exists = appGroupMemberRepository.findByGroupIdAndUserId(groupId, userId).isPresent();
        if (exists) { return ResponseEntity.badRequest().body("User already in group"); }
        AppGroupMember member = new AppGroupMember();
        member.setGroupId(groupId);
        member.setAppId(awd.app.getId());
        member.setUserId(userId);
        member.setAssignedBy(currentPrincipalId());
        appGroupMemberRepository.save(member);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<?> listUserGroups(@PathVariable String slug, @PathVariable String appSlug,
            @PathVariable String userId) {
        AppWithDomain awd = requireApplication(slug, appSlug);
        String currentUserId = currentPrincipalId();
        boolean isOwn = userId.equals(currentUserId);
        if (!isOwn && !permissionService.hasAppPermission(awd.app.getId(), AppPermission.APP_WRITE) &&
            !permissionService.hasDomainPermission(awd.org.getId(), DomainPermission.DOMAIN_MANAGE_APPS)) {
            return ResponseEntity.status(403).build();
        }
        var userOpt = domainUserRepository.findById(userId);
        if (userOpt.isEmpty() || !userOpt.get().getDomainId().equals(awd.org.getId())) {
            return ResponseEntity.notFound().build();
        }
        List<AppGroupMember> memberships = appGroupMemberRepository
                .findByAppIdAndUserId(awd.app.getId(), userId);
        List<AppGroup> userGroups = memberships.stream()
                .map(m -> appGroupRepository.findById(m.getGroupId()).orElse(null))
                .filter(g -> g != null).collect(Collectors.toList());
        return ResponseEntity.ok(userGroups);
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable String slug, @PathVariable String appSlug,
            @PathVariable String groupId, @PathVariable String userId) {
        AppWithDomain awd = requireApplication(slug, appSlug);
        if (!permissionService.hasAppPermission(awd.app.getId(), AppPermission.APP_WRITE) &&
            !permissionService.hasDomainPermission(awd.org.getId(), DomainPermission.DOMAIN_MANAGE_APPS)) {
            return ResponseEntity.status(403).build();
        }
        AppGroup group = appGroupRepository.findById(groupId).orElse(null);
        if (group == null || !group.getAppId().equals(awd.app.getId())) {
            return ResponseEntity.notFound().build();
        }
        appGroupMemberRepository.findByGroupIdAndUserId(groupId, userId).ifPresent(appGroupMemberRepository::delete);
        List<AppGroupMember> remaining = appGroupMemberRepository.findByAppIdAndUserId(awd.app.getId(), userId);
        if (remaining.isEmpty()) {
            List<AppGroup> groups = appGroupRepository.findByAppId(awd.app.getId());
            AppGroup defaultViewer = groups.stream()
                    .filter(g -> g.isDefaultGroup() && "App Viewer".equalsIgnoreCase(g.getName()))
                    .findFirst().orElse(null);
            if (defaultViewer != null) {
                AppGroupMember viewerM = new AppGroupMember();
                viewerM.setGroupId(defaultViewer.getId());
                viewerM.setAppId(awd.app.getId());
                viewerM.setUserId(userId);
                viewerM.setAssignedBy("system");
                appGroupMemberRepository.save(viewerM);
            }
        }
        return ResponseEntity.ok().build();
    }

    private AppWithDomain requireApplication(String slug, String appSlug) {
        Organisation org = organisationRepository.findBySlug(slugify(slug))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found"));
        Application app = applicationRepository.findByDomainIdAndSlug(org.getId(), appSlug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        return new AppWithDomain(org, app);
    }

    private record AppWithDomain(Organisation org, Application app) {}

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
