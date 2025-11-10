package com.formgenerator.api.dto.app;

import jakarta.validation.constraints.NotBlank;

public class CreateApplicationRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private String ownerUserId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }
}
