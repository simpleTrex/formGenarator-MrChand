package com.formgenerator.api.models.app;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.formgenerator.api.permissions.AppPermission;

@Document(collection = "app_groups")
@CompoundIndexes({
        @CompoundIndex(name = "app_group_name_idx", def = "{'appId':1,'name':1}", unique = true)
})
public class AppGroup {

    @Id
    private String id;

    private String appId;

    private String name;

    private Set<AppPermission> permissions = EnumSet.noneOf(AppPermission.class);

    private boolean defaultGroup;

    private Instant createdAt = Instant.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

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

    public boolean isDefaultGroup() {
        return defaultGroup;
    }

    public void setDefaultGroup(boolean defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
