package com.formgenerator.api.services;

import java.util.EnumSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.formgenerator.api.models.app.AppGroup;
import com.formgenerator.api.models.app.AppGroupMember;
import com.formgenerator.api.models.app.Application;
import com.formgenerator.api.permissions.AppPermission;
import com.formgenerator.api.repository.AppGroupMemberRepository;
import com.formgenerator.api.repository.AppGroupRepository;

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
                    EnumSet.of(AppPermission.APP_READ, AppPermission.APP_WRITE, AppPermission.APP_EXECUTE), true);
            AppGroup editor = buildGroup(application.getId(), "App Editor",
                    EnumSet.of(AppPermission.APP_READ, AppPermission.APP_WRITE), true);
            AppGroup viewer = buildGroup(application.getId(), "App Viewer",
                    EnumSet.of(AppPermission.APP_READ), true);
            appGroupRepository.saveAll(List.of(admin, editor, viewer));
            if (ownerUserId != null) {
                assignUser(admin, application.getId(), ownerUserId, ownerUserId);
            }
        }
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
