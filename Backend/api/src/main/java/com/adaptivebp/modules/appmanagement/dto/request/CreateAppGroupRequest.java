package com.adaptivebp.modules.appmanagement.dto.request;

import java.util.Set;

import com.adaptivebp.modules.appmanagement.permission.AppPermission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public class CreateAppGroupRequest {
    @NotBlank
    private String name;
    @NotEmpty
    private Set<AppPermission> permissions;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Set<AppPermission> getPermissions() { return permissions; }
    public void setPermissions(Set<AppPermission> permissions) { this.permissions = permissions; }
}
