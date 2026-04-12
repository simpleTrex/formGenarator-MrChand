package com.adaptivebp.modules.workflow.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.adaptivebp.modules.workflow.model.enums.WorkflowStatus;

@Document(collection = "workflow_definitions")
@CompoundIndexes({
        @CompoundIndex(name = "domain_app_slug_version_idx", def = "{'domainId':1,'appId':1,'slug':1,'version':1}", unique = true),
        @CompoundIndex(name = "domain_app_status_idx", def = "{'domainId':1,'appId':1,'status':1}")
})
public class WorkflowDefinition {

    @Id
    private String id;
    private String domainId;
    private String appId;
    private String name;
    private String slug;
    private String description;
    private int version = 1;
    private WorkflowStatus status = WorkflowStatus.DRAFT;
    private List<WorkflowStep> steps = new ArrayList<>();
    private List<WorkflowEdge> globalEdges = new ArrayList<>();
    private String createdBy;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public Optional<WorkflowStep> findStartStep() {
        return steps.stream().filter(WorkflowStep::isStart).findFirst();
    }

    public WorkflowStep findStepById(String stepId) {
        if (stepId == null) {
            return null;
        }
        return steps.stream().filter(s -> stepId.equals(s.getId())).findFirst().orElse(null);
    }

    public List<WorkflowStep> orderedSteps() {
        return steps.stream().sorted(Comparator.comparingInt(WorkflowStep::getOrder)).toList();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public List<WorkflowStep> getSteps() {
        return steps;
    }

    public void setSteps(List<WorkflowStep> steps) {
        this.steps = steps != null ? steps : new ArrayList<>();
    }

    public List<WorkflowEdge> getGlobalEdges() {
        return globalEdges;
    }

    public void setGlobalEdges(List<WorkflowEdge> globalEdges) {
        this.globalEdges = globalEdges != null ? globalEdges : new ArrayList<>();
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
