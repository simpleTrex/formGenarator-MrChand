package com.adaptivebp.modules.uibuilder.dto;

import jakarta.validation.constraints.NotBlank;

public class CreatePageRequest {
    @NotBlank
    private String name;
    private String slug;
    private int order = 0;

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

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
