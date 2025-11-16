package com.formgenerator.api.responses;

import java.util.Set;

import com.formgenerator.api.permissions.DomainPermission;
import com.formgenerator.platform.auth.PrincipalType;

public class DomainAccessResponse {
    private Set<DomainPermission> permissions;
    private java.util.List<String> groups;
    private PrincipalType principalType;

    public DomainAccessResponse(Set<DomainPermission> permissions, java.util.List<String> groups,
            PrincipalType principalType) {
        this.permissions = permissions;
        this.groups = groups;
        this.principalType = principalType;
    }

    public Set<DomainPermission> getPermissions() {
        return permissions;
    }

    public java.util.List<String> getGroups() {
        return groups;
    }

    public PrincipalType getPrincipalType() {
        return principalType;
    }
}
