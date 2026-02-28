package com.adaptivebp.modules.organisation.dto.request;

import jakarta.validation.constraints.NotBlank;

public class AssignMemberRequest {
    @NotBlank
    private String username;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
