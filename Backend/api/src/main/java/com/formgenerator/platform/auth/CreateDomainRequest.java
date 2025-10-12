package com.formgenerator.platform.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new domain.
 */
public class CreateDomainRequest {
    @NotBlank
    @Size(min = 3, max = 50, message = "Domain name must be between 3 and 50 characters")
    private String name;

    public CreateDomainRequest() {}

    public CreateDomainRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}