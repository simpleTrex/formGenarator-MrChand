package com.formgenerator.api.dto.model;

import java.util.List;
import java.util.Set;

import com.formgenerator.api.models.domain.model.DomainModelField;

import jakarta.validation.Valid;

public class UpdateDomainModelRequest {

    private String name;

    private String description;

    /**
     * If provided, update access policy.
     */
    private Boolean sharedWithAllApps;

    private Set<String> allowedAppIds;

    /**
     * Full field list replacement (simple MVP). Later we can support patch operations.
     */
    @Valid
    private List<DomainModelField> fields;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getSharedWithAllApps() {
        return sharedWithAllApps;
    }

    public void setSharedWithAllApps(Boolean sharedWithAllApps) {
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
