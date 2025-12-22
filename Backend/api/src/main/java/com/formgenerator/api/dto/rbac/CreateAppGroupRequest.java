package com.formgenerator.api.dto.rbac;

import java.util.Set;

import com.formgenerator.api.permissions.AppPermission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public class CreateAppGroupRequest {

    @NotBlank
    private String name;

    @NotEmpty
    private Set<AppPermission> permissions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<AppPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<AppPermission> permissions) {
        this.permissions = permissions;
    }
}
