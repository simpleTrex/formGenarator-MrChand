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
        if (application == null || application.getId() == null) {
            return;
        }
        List<AppGroup> existing = appGroupRepository.findByAppId(application.getId());
        if (existing.isEmpty()) {
            AppGroup admin = buildGroup(application.getId(), "App Admin",
                    EnumSet.of(AppPermission.APP_READ, AppPermission.APP_WRITE, AppPermission.APP_EXECUTE,
                            AppPermission.APP_MANAGE_PROCESSES, AppPermission.APP_START_PROCESS,
                            AppPermission.APP_VIEW_PROCESSES), true);
            AppGroup editor = buildGroup(application.getId(), "App Editor",
                    EnumSet.of(AppPermission.APP_READ, AppPermission.APP_WRITE,
                            AppPermission.APP_MANAGE_PROCESSES, AppPermission.APP_START_PROCESS,
                            AppPermission.APP_VIEW_PROCESSES), true);
            AppGroup viewer = buildGroup(application.getId(), "App Viewer",
                    EnumSet.of(AppPermission.APP_READ, AppPermission.APP_VIEW_PROCESSES), true);
            appGroupRepository.saveAll(List.of(admin, editor, viewer));
            if (ownerUserId != null) {
                assignUser(admin, application.getId(), ownerUserId, ownerUserId);
            }
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
        if (group == null || userId == null) {
            return;
        }
        boolean exists = appGroupMemberRepository.findByGroupIdAndUserId(group.getId(), userId).isPresent();
        if (!exists) {
            AppGroupMember member = new AppGroupMember();
            member.setGroupId(group.getId());
            member.setAppId(appId);
            member.setUserId(userId);
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
}
