package com.formgenerator.api.models.domain;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.formgenerator.api.permissions.DomainPermission;

@Document(collection = "domain_groups")
@CompoundIndexes({
        @CompoundIndex(name = "domain_group_name_idx", def = "{'domainId':1,'name':1}", unique = true)
})
public class DomainGroup {

    @Id
    private String id;

    private String domainId;

    private String name;

    private Set<DomainPermission> permissions = EnumSet.noneOf(DomainPermission.class);

    private boolean defaultGroup;

    private Instant createdAt = Instant.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

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
