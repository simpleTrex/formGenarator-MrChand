package com.formgenerator.api.models.domain.model;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DomainModelField {

    @NotBlank
    private String key;

    @NotNull
    private DomainFieldType type;

    private boolean required;

    private boolean unique;

    /**
     * Flexible validation/properties map. Examples:
     * - {"min":0,"max":100}
     * - {"regex":"^[a-z]+$"}
     * - for REFERENCE: {"targetModelSlug":"customer","displayField":"name"}
     */
    private Map<String, Object> config = new HashMap<>();

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public DomainFieldType getType() {
        return type;
    }

    public void setType(DomainFieldType type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }
}
