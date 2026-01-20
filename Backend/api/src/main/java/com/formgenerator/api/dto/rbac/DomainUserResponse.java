package com.formgenerator.api.dto.rbac;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.formgenerator.api.models.domain.DomainUser;

public class DomainUserResponse {

    private String id;
    private String username;
    private String email;
    private String status;
    private Map<String, Object> profile;
    private Instant createdAt;
    private List<GroupMembershipInfo> groups = new ArrayList<>();

    public DomainUserResponse() {
    }

    public DomainUserResponse(DomainUser user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.status = user.getStatus();
        this.profile = user.getProfile();
        this.createdAt = user.getCreatedAt();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getProfile() {
        return profile;
    }

    public void setProfile(Map<String, Object> profile) {
        this.profile = profile;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<GroupMembershipInfo> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupMembershipInfo> groups) {
        this.groups = groups;
    }

    public static class GroupMembershipInfo {
        private String groupId;
        private String groupName;
        private Instant assignedAt;

        public GroupMembershipInfo() {
        }

        public GroupMembershipInfo(String groupId, String groupName, Instant assignedAt) {
            this.groupId = groupId;
            this.groupName = groupName;
            this.assignedAt = assignedAt;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getGroupName() {
            return groupName;
        }

        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }

        public Instant getAssignedAt() {
            return assignedAt;
        }

        public void setAssignedAt(Instant assignedAt) {
            this.assignedAt = assignedAt;
        }
    }
}
