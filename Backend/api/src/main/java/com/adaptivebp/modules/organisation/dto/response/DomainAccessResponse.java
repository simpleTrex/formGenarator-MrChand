package com.adaptivebp.modules.organisation.dto.response;

import java.util.List;
import java.util.Set;

import com.adaptivebp.modules.organisation.permission.DomainPermission;
import com.adaptivebp.shared.security.PrincipalType;

public class DomainAccessResponse {
    private Set<DomainPermission> permissions;
    private List<String> groups;
    private PrincipalType principalType;

    public DomainAccessResponse(Set<DomainPermission> permissions, List<String> groups,
            PrincipalType principalType) {
        this.permissions = permissions;
        this.groups = groups;
        this.principalType = principalType;
    }

    public Set<DomainPermission> getPermissions() { return permissions; }
    public List<String> getGroups() { return groups; }
    public PrincipalType getPrincipalType() { return principalType; }
}
