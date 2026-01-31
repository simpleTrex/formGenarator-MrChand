package com.formgenerator.platform.workflow;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

/**
 * WorkflowInstance represents a running instance of a workflow for a specific
 * record.
 */
@Document(collection = "workflow_instances")
public class WorkflowInstance {
    @Id
    private String id;

    @Indexed
    private String workflowDefinitionId;

    @Indexed
    private String domainId;

    private String modelId;

    // Reference to the actual business data record
    @Indexed
    private String recordId;

    // Current state ID
    @Indexed
    private String currentState;

    private String previousState;

    // Assignment info
    private AssignmentInfo assignedTo;

    // Business data (stored as flexible map)
    private Map<String, Object> data = new HashMap<>();

    // Transition history
    private List<TransitionHistory> history = new ArrayList<>();

    // Comments/notes on this workflow instance
    private List<Comment> comments = new ArrayList<>();

    // Attachments
    private List<Attachment> attachments = new ArrayList<>();

    private Date createdAt;
    private Date updatedAt;
    private String createdBy;

    public WorkflowInstance() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkflowDefinitionId() {
        return workflowDefinitionId;
    }

    public void setWorkflowDefinitionId(String workflowDefinitionId) {
        this.workflowDefinitionId = workflowDefinitionId;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    public String getPreviousState() {
        return previousState;
    }

    public void setPreviousState(String previousState) {
        this.previousState = previousState;
    }

    public AssignmentInfo getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(AssignmentInfo assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public List<TransitionHistory> getHistory() {
        return history;
    }

    public void setHistory(List<TransitionHistory> history) {
        this.history = history;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Assignment information for this workflow instance
     */
    public static class AssignmentInfo {
        private String userId;
        private String role;
        private Date assignedAt;

        public AssignmentInfo() {
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public Date getAssignedAt() {
            return assignedAt;
        }

        public void setAssignedAt(Date assignedAt) {
            this.assignedAt = assignedAt;
        }
    }

    /**
     * Record of a state transition
     */
    public static class TransitionHistory {
        private String transitionId;
        private String fromState;
        private String toState;
        private String performedBy;
        private Date performedAt;
        private String comment;
        private List<BusinessActionResult> businessActionsExecuted = new ArrayList<>();

        public TransitionHistory() {
        }

        // Getters and Setters
        public String getTransitionId() {
            return transitionId;
        }

        public void setTransitionId(String transitionId) {
            this.transitionId = transitionId;
        }

        public String getFromState() {
            return fromState;
        }

        public void setFromState(String fromState) {
            this.fromState = fromState;
        }

        public String getToState() {
            return toState;
        }

        public void setToState(String toState) {
            this.toState = toState;
        }

        public String getPerformedBy() {
            return performedBy;
        }

        public void setPerformedBy(String performedBy) {
            this.performedBy = performedBy;
        }

        public Date getPerformedAt() {
            return performedAt;
        }

        public void setPerformedAt(Date performedAt) {
            this.performedAt = performedAt;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public List<BusinessActionResult> getBusinessActionsExecuted() {
            return businessActionsExecuted;
        }

        public void setBusinessActionsExecuted(List<BusinessActionResult> businessActionsExecuted) {
            this.businessActionsExecuted = businessActionsExecuted;
        }
    }

    /**
     * Result of a business action execution
     */
    public static class BusinessActionResult {
        private String type;
        private String status; // SUCCESS, FAILED, SKIPPED
        private String message;
        private Map<String, Object> output;

        public BusinessActionResult() {
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Map<String, Object> getOutput() {
            return output;
        }

        public void setOutput(Map<String, Object> output) {
            this.output = output;
        }
    }

    /**
     * Comment on workflow instance
     */
    public static class Comment {
        private String id;
        private String userId;
        private String text;
        private Date createdAt;

        public Comment() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
        }
    }

    /**
     * Attachment on workflow instance
     */
    public static class Attachment {
        private String id;
        private String name;
        private String url;
        private String uploadedBy;
        private Date uploadedAt;

        public Attachment() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUploadedBy() {
            return uploadedBy;
        }

        public void setUploadedBy(String uploadedBy) {
            this.uploadedBy = uploadedBy;
        }

        public Date getUploadedAt() {
            return uploadedAt;
        }

        public void setUploadedAt(Date uploadedAt) {
            this.uploadedAt = uploadedAt;
        }
    }
}
