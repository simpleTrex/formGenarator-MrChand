package com.formgenerator.api.dto.rbac;

import java.util.Set;

import com.formgenerator.api.permissions.DomainPermission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public class CreateDomainGroupRequest {

    @NotBlank
    private String name;

    @NotEmpty
    private Set<DomainPermission> permissions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<DomainPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<DomainPermission> permissions) {
        this.permissions = permissions;
    }
}
