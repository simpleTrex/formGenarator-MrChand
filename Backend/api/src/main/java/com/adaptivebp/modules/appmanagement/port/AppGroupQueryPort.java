package com.adaptivebp.modules.appmanagement.port;

import java.util.Set;

import com.adaptivebp.modules.appmanagement.permission.AppPermission;

/**
 * Public API that the appmanagement module exposes for permission queries.
 * Other modules (e.g. organisation's PermissionService) must depend on this
 * interface — never on AppGroupRepository / AppGroupMemberRepository directly.
 */
public interface AppGroupQueryPort {
    /**
     * Returns the full set of AppPermissions the given user holds across
     * all app groups they are a member of for the given application.
     */
    Set<AppPermission> getAppPermissions(String appId, String userId);

    /**
     * Checks whether a user is a member of the given app group.
     */
    boolean isMemberOfAppGroup(String groupId, String userId);
}
