package com.adaptivebp.modules.appmanagement.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.adaptivebp.modules.appmanagement.model.AppGroup;
import com.adaptivebp.modules.appmanagement.model.AppGroupMember;
import com.adaptivebp.modules.appmanagement.permission.AppPermission;
import com.adaptivebp.modules.appmanagement.port.AppGroupQueryPort;
import com.adaptivebp.modules.appmanagement.repository.AppGroupMemberRepository;
import com.adaptivebp.modules.appmanagement.repository.AppGroupRepository;

/**
 * Implements AppGroupQueryPort — the public permission-query API for the
 * appmanagement module. Other modules (e.g. PermissionService in organisation)
 * inject this via the port interface, never touching the repositories directly.
 */
@Service
public class AppGroupQueryService implements AppGroupQueryPort {

    @Autowired
    private AppGroupMemberRepository appGroupMemberRepository;

    @Autowired
    private AppGroupRepository appGroupRepository;

    @Override
    public Set<AppPermission> getAppPermissions(String appId, String userId) {
        List<AppGroupMember> memberships = appGroupMemberRepository.findByAppIdAndUserId(appId, userId);
        if (memberships.isEmpty()) {
            return EnumSet.noneOf(AppPermission.class);
        }
        Set<String> groupIds = memberships.stream()
                .map(AppGroupMember::getGroupId)
                .collect(Collectors.toSet());
        List<AppGroup> groups = appGroupRepository.findAllById(groupIds);
        Set<AppPermission> permissions = EnumSet.noneOf(AppPermission.class);
        for (AppGroup group : groups) {
            if (group.getPermissions() != null) {
                permissions.addAll(group.getPermissions());
            }
        }
        return permissions;
    }

    @Override
    public boolean isMemberOfAppGroup(String groupId, String userId) {
        return appGroupMemberRepository.findByGroupIdAndUserId(groupId, userId).isPresent();
    }
}
