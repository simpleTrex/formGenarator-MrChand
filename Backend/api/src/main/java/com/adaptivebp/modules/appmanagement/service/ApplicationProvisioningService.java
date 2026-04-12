package com.adaptivebp.modules.appmanagement.service;

import java.util.EnumSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.adaptivebp.modules.appmanagement.model.AppGroup;
import com.adaptivebp.modules.appmanagement.model.AppGroupMember;
import com.adaptivebp.modules.appmanagement.model.Application;
import com.adaptivebp.modules.appmanagement.permission.AppPermission;
import com.adaptivebp.modules.appmanagement.repository.AppGroupMemberRepository;
import com.adaptivebp.modules.appmanagement.repository.AppGroupRepository;

@Service
public class ApplicationProvisioningService {

    @Autowired
    private AppGroupRepository appGroupRepository;

    @Autowired
    private AppGroupMemberRepository appGroupMemberRepository;

    public void provisionDefaultGroups(Application application, String ownerUserId) {
        provisionDefaultGroups(application, ownerUserId, null);
    }

    public void provisionDefaultGroups(Application application, String ownerUserId, String creatorUserId) {
        if (application == null || application.getId() == null) {
            return;
        }
        List<AppGroup> groups = appGroupRepository.findByAppId(application.getId());
        if (groups.isEmpty()) {
            AppGroup admin = buildGroup(application.getId(), "App Admin",
                EnumSet.of(
                    AppPermission.APP_VIEW,
                    AppPermission.APP_CONFIGURE,
                    AppPermission.APP_MANAGE_WORKFLOW,
                    AppPermission.APP_START_WORKFLOW,
                    AppPermission.APP_EXECUTE_WORKFLOW,
                    AppPermission.APP_VIEW_ALL_INSTANCES), true);
            AppGroup editor = buildGroup(application.getId(), "App Editor",
                EnumSet.of(
                    AppPermission.APP_VIEW,
                    AppPermission.APP_CONFIGURE,
                    AppPermission.APP_MANAGE_WORKFLOW,
                    AppPermission.APP_START_WORKFLOW,
                    AppPermission.APP_EXECUTE_WORKFLOW), true);
            AppGroup viewer = buildGroup(application.getId(), "App Viewer",
            EnumSet.of(
                AppPermission.APP_VIEW,
                AppPermission.APP_START_WORKFLOW,
                AppPermission.APP_EXECUTE_WORKFLOW), true);
            groups = appGroupRepository.saveAll(List.of(admin, editor, viewer));
        }

        AppGroup adminGroup = groups.stream()
                .filter(g -> g.isDefaultGroup() && "App Admin".equalsIgnoreCase(g.getName()))
                .findFirst()
                .orElse(null);
        if (adminGroup == null) {
            return;
        }

        String normalizedOwner = normalizeUserId(ownerUserId);
        String normalizedCreator = normalizeUserId(creatorUserId);

        if (normalizedOwner != null) {
            assignUser(adminGroup, application.getId(), normalizedOwner, normalizedOwner);
        }
        if (normalizedCreator != null && !normalizedCreator.equals(normalizedOwner)) {
            assignUser(adminGroup, application.getId(), normalizedCreator, normalizedCreator);
        }
    }

    /**
     * Ensures a user is assigned to the default "App Viewer" group when they have no
     * group memberships. Intended to be called explicitly from write operations (e.g.,
     * a dedicated POST endpoint or after member removal) — never from a GET handler.
     *
     * @return the saved AppGroupMember if a new assignment was created, null otherwise
     */
    public AppGroupMember ensureDefaultViewerMembership(String appId, String userId) {
        if (appId == null || userId == null) {
            return null;
        }
        List<AppGroupMember> existing = appGroupMemberRepository.findByAppIdAndUserId(appId, userId);
        if (!existing.isEmpty()) {
            return null; // user already has at least one group
        }
        List<AppGroup> groups = appGroupRepository.findByAppId(appId);
        AppGroup defaultViewer = groups.stream()
                .filter(g -> g.isDefaultGroup() && "App Viewer".equalsIgnoreCase(g.getName()))
                .findFirst().orElse(null);
        if (defaultViewer == null) {
            return null;
        }
        AppGroupMember member = new AppGroupMember();
        member.setGroupId(defaultViewer.getId());
        member.setAppId(appId);
        member.setUserId(userId);
        member.setAssignedBy("system");
        return appGroupMemberRepository.save(member);
    }

    public void assignUser(AppGroup group, String appId, String userId, String assignedBy) {
        if (group == null) {
            return;
        }
        String normalizedUser = normalizeUserId(userId);
        if (normalizedUser == null) {
            return;
        }
        boolean exists = appGroupMemberRepository.findByGroupIdAndUserId(group.getId(), normalizedUser).isPresent();
        if (!exists) {
            AppGroupMember member = new AppGroupMember();
            member.setGroupId(group.getId());
            member.setAppId(appId);
            member.setUserId(normalizedUser);
            member.setAssignedBy(assignedBy);
            appGroupMemberRepository.save(member);
        }
    }

    private AppGroup buildGroup(String appId, String name, EnumSet<AppPermission> permissions, boolean defaultGroup) {
        AppGroup group = new AppGroup();
        group.setAppId(appId);
        group.setName(name);
        group.setPermissions(permissions);
        group.setDefaultGroup(defaultGroup);
        return group;
    }

    private String normalizeUserId(String userId) {
        if (userId == null) {
            return null;
        }
        String normalized = userId.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
