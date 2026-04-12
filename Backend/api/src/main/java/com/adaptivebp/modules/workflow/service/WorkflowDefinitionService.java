package com.adaptivebp.modules.workflow.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.adaptivebp.modules.workflow.dto.request.CreateWorkflowRequest;
import com.adaptivebp.modules.workflow.dto.request.UpdateWorkflowRequest;
import com.adaptivebp.modules.workflow.dto.response.ValidationResult;
import com.adaptivebp.modules.workflow.exception.WorkflowNotFoundException;
import com.adaptivebp.modules.workflow.exception.WorkflowValidationException;
import com.adaptivebp.modules.workflow.model.WorkflowDefinition;
import com.adaptivebp.modules.workflow.model.enums.InstanceStatus;
import com.adaptivebp.modules.workflow.model.enums.WorkflowStatus;
import com.adaptivebp.modules.workflow.repository.WorkflowDefinitionRepository;
import com.adaptivebp.modules.workflow.repository.WorkflowInstanceRepository;

@Service
public class WorkflowDefinitionService {

    @Autowired
    private WorkflowDefinitionRepository definitionRepository;

    @Autowired
    private WorkflowInstanceRepository instanceRepository;

    @Autowired
    private WorkflowValidationService validationService;

    public WorkflowDefinition createWorkflow(String domainId, String appId, CreateWorkflowRequest request, String createdBy) {
        Optional<WorkflowDefinition> existing = definitionRepository.findTopByDomainIdAndAppIdOrderByVersionDesc(domainId, appId);
        if (existing.isPresent()) {
            throw new IllegalStateException("This application already has a workflow. Update the existing one instead.");
        }
        String slug = slugify(request.getName());

        Optional<WorkflowDefinition> latest = definitionRepository
                .findTopByDomainIdAndAppIdAndSlugOrderByVersionDesc(domainId, appId, slug);

        int nextVersion = latest.map(d -> d.getVersion() + 1).orElse(1);

        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setDomainId(domainId);
        definition.setAppId(appId);
        definition.setName(request.getName());
        definition.setSlug(slug);
        definition.setDescription(request.getDescription());
        definition.setSteps(request.getSteps());
        definition.setGlobalEdges(request.getGlobalEdges());
        definition.setStatus(WorkflowStatus.DRAFT);
        definition.setVersion(nextVersion);
        definition.setCreatedBy(createdBy);
        definition.setCreatedAt(Instant.now());
        definition.setUpdatedAt(Instant.now());

        return definitionRepository.save(definition);
    }

    public WorkflowDefinition updateWorkflow(String domainId,
            String appId,
            String workflowSlug,
            UpdateWorkflowRequest request) {
        WorkflowDefinition definition = requireLatestBySlug(domainId, appId, workflowSlug);
        if (definition.getStatus() == WorkflowStatus.PUBLISHED) {
            throw new IllegalStateException("Published workflows must be archived before editing");
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            definition.setName(request.getName());
        }
        if (request.getDescription() != null) {
            definition.setDescription(request.getDescription());
        }
        if (request.getSteps() != null) {
            definition.setSteps(request.getSteps());
        }
        if (request.getGlobalEdges() != null) {
            definition.setGlobalEdges(request.getGlobalEdges());
        }

        definition.setUpdatedAt(Instant.now());
        return definitionRepository.save(definition);
    }

    public WorkflowDefinition publishWorkflow(String domainId, String appId, String workflowSlug) {
        WorkflowDefinition draft = requireLatestBySlug(domainId, appId, workflowSlug);
        if (draft.getStatus() == WorkflowStatus.PUBLISHED) {
            throw new IllegalStateException("Workflow is already published");
        }

        ValidationResult validation = validationService.validate(draft);
        if (!validation.isValid()) {
            throw new WorkflowValidationException(validation.getErrors());
        }

        List<WorkflowDefinition> sameSlug = definitionRepository.findByDomainIdAndAppId(domainId, appId).stream()
                .filter(d -> workflowSlug.equalsIgnoreCase(d.getSlug()))
                .toList();

        sameSlug.stream()
                .filter(d -> d.getStatus() == WorkflowStatus.PUBLISHED)
                .forEach(existingPublished -> {
                    existingPublished.setStatus(WorkflowStatus.ARCHIVED);
                    existingPublished.setUpdatedAt(Instant.now());
                    definitionRepository.save(existingPublished);
                });

        int maxVersion = sameSlug.stream().map(WorkflowDefinition::getVersion).max(Comparator.naturalOrder()).orElse(0);
        if (draft.getVersion() <= maxVersion) {
            draft.setVersion(maxVersion + 1);
        }
        draft.setStatus(WorkflowStatus.PUBLISHED);
        draft.setUpdatedAt(Instant.now());

        return definitionRepository.save(draft);
    }

    public WorkflowDefinition archiveWorkflow(String domainId, String appId, String workflowSlug) {
        WorkflowDefinition published = definitionRepository
                .findByDomainIdAndAppIdAndSlugAndStatus(domainId, appId, workflowSlug, WorkflowStatus.PUBLISHED)
                .orElseThrow(() -> new WorkflowNotFoundException("No published workflow found for slug: " + workflowSlug));

        long activeInstances = instanceRepository.countByWorkflowDefinitionIdAndStatus(
                published.getId(),
                InstanceStatus.ACTIVE);
        if (activeInstances > 0) {
            // Allow archive; running instances keep pointing to their version snapshot.
        }

        published.setStatus(WorkflowStatus.ARCHIVED);
        published.setUpdatedAt(Instant.now());
        return definitionRepository.save(published);
    }

    public WorkflowDefinition getWorkflow(String domainId, String appId, String workflowSlug, Integer version) {
        if (version == null) {
            return requireLatestBySlug(domainId, appId, workflowSlug);
        }

        return definitionRepository.findByDomainIdAndAppId(domainId, appId).stream()
                .filter(d -> workflowSlug.equalsIgnoreCase(d.getSlug()) && d.getVersion() == version)
                .findFirst()
                .orElseThrow(() -> new WorkflowNotFoundException(
                        "Workflow not found for slug " + workflowSlug + " and version " + version));
    }

    public List<WorkflowDefinition> listWorkflows(String domainId, String appId) {
        Optional<WorkflowDefinition> latest = definitionRepository.findTopByDomainIdAndAppIdOrderByVersionDesc(domainId, appId);
        if (latest.isEmpty()) {
            return List.of();
        }
        return List.of(latest.get());
    }

    public void deleteWorkflow(String domainId, String appId, String workflowSlug) {
        WorkflowDefinition definition = requireLatestBySlug(domainId, appId, workflowSlug);
        if (definition.getStatus() != WorkflowStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT workflows can be deleted");
        }
        definitionRepository.delete(definition);
    }

    public WorkflowDefinition getPublishedWorkflow(String domainId, String appId, String workflowSlug) {
        return definitionRepository.findByDomainIdAndAppIdAndSlugAndStatus(
                        domainId, appId, workflowSlug, WorkflowStatus.PUBLISHED)
                .orElseThrow(() -> new WorkflowNotFoundException("Published workflow not found: " + workflowSlug));
    }

    private WorkflowDefinition requireLatestBySlug(String domainId, String appId, String workflowSlug) {
        return definitionRepository.findTopByDomainIdAndAppIdAndSlugOrderByVersionDesc(
                        domainId, appId, workflowSlug)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found: " + workflowSlug));
    }

    public String slugify(String value) {
        String s = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+", "").replaceAll("-+$", "");
        if (s.isBlank()) {
            return "workflow";
        }
        return s;
    }
}
