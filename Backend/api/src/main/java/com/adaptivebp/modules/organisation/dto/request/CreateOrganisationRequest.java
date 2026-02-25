package com.adaptivebp.modules.organisation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateOrganisationRequest {
    @NotBlank
    @Size(min = 3, max = 50, message = "Organisation name must be between 3 and 50 characters")
    private String name;

    @NotBlank
    @Size(min = 3, max = 50, message = "Slug must be between 3 and 50 characters")
    private String slug;

    @Size(max = 500)
    private String description;

    @Size(max = 100)
    private String industry;

    public CreateOrganisationRequest() {}

    public CreateOrganisationRequest(String name, String slug, String description, String industry) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.industry = industry;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
}
