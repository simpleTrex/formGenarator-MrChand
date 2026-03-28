package com.adaptivebp.modules.workflow.dto.request;

import java.util.HashMap;
import java.util.Map;

public class StartWorkflowRequest {
    private Map<String, Object> formData = new HashMap<>();

    public Map<String, Object> getFormData() {
        return formData;
    }

    public void setFormData(Map<String, Object> formData) {
        this.formData = formData != null ? formData : new HashMap<>();
    }
}
