package com.adaptivebp.modules.process.dto;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;

public class SubmitNodeRequest {

    @NotBlank(message = "nodeId is required")
    private String nodeId;

    private Map<String, Object> formData = new HashMap<>();

    /** For APPROVAL nodes: the action id (e.g. "approve", "reject") */
    private String action;

    private String comment;

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public Map<String, Object> getFormData() { return formData; }
    public void setFormData(Map<String, Object> formData) { this.formData = formData; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
