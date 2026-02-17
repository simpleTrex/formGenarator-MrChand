package com.formgenerator.api.dto.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.formgenerator.api.models.domain.model.DomainModelField;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public class CreateDomainModelRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private String description;

    private boolean sharedWithAllApps = false;

    private Set<String> allowedAppIds;

    @Valid
    private List<DomainModelField> fields = new ArrayList<>();

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSharedWithAllApps() {
        return sharedWithAllApps;
    }

    public void setSharedWithAllApps(boolean sharedWithAllApps) {
        this.sharedWithAllApps = sharedWithAllApps;
    }

    public Set<String> getAllowedAppIds() {
        return allowedAppIds;
    }

    public void setAllowedAppIds(Set<String> allowedAppIds) {
        this.allowedAppIds = allowedAppIds;
    }

    public List<DomainModelField> getFields() {
        return fields;
    }

    public void setFields(List<DomainModelField> fields) {
        this.fields = fields;
    }
}
