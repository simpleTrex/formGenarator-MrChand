package com.adaptivebp.modules.organisation.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CreateWorkflowRoleRequest {
    @NotBlank
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
