package com.formgenerator.api.dto.rbac;

import java.time.Instant;

public class GroupMemberResponse {

    private String userId;
    private String username;
    private String email;
    private String status;
    private Instant assignedAt;
    private String assignedBy;

    public GroupMemberResponse() {
    }

    public GroupMemberResponse(String userId, String username, String email, String status, 
                               Instant assignedAt, String assignedBy) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.status = status;
        this.assignedAt = assignedAt;
        this.assignedBy = assignedBy;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }
}
