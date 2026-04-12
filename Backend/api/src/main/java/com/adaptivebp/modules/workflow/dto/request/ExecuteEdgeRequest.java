package com.adaptivebp.modules.workflow.dto.request;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;

public class ExecuteEdgeRequest {
    @NotBlank
    private String edgeId;
    private Map<String, Object> formData = new HashMap<>();
    private String comment;

    public String getEdgeId() {
        return edgeId;
    }

    public void setEdgeId(String edgeId) {
        this.edgeId = edgeId;
    }

    public Map<String, Object> getFormData() {
        return formData;
    }

    public void setFormData(Map<String, Object> formData) {
        this.formData = formData != null ? formData : new HashMap<>();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
