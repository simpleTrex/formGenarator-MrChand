package com.formgenerator.api.models.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "domain_group_members")
@CompoundIndexes({
        @CompoundIndex(name = "domain_group_user_idx", def = "{'domainGroupId':1,'userId':1}", unique = true),
        @CompoundIndex(name = "domain_user_idx", def = "{'domainId':1,'userId':1}")
})
public class DomainGroupMember {

    @Id
    private String id;

    private String domainGroupId;

    private String domainId;

    private String userId;

    private String assignedBy;

    private Instant assignedAt = Instant.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDomainGroupId() {
        return domainGroupId;
    }

    public void setDomainGroupId(String domainGroupId) {
        this.domainGroupId = domainGroupId;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }
}
