package com.adaptivebp.modules.process.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.adaptivebp.modules.process.dto.CreateProcessRequest;
import com.adaptivebp.modules.process.dto.UpdateProcessRequest;
import com.adaptivebp.modules.process.dto.ValidationResult;
import com.adaptivebp.modules.process.exception.ProcessNotFoundException;
import com.adaptivebp.modules.process.exception.ProcessValidationException;
import com.adaptivebp.modules.process.model.ProcessDefinition;
import com.adaptivebp.modules.process.model.enums.InstanceStatus;
import com.adaptivebp.modules.process.model.enums.ProcessStatus;
import com.adaptivebp.modules.process.repository.ProcessDefinitionRepository;
import com.adaptivebp.modules.process.repository.ProcessInstanceRepository;

/**
 * Each application can have multiple process definitions, distinguished by slug.
 */
@Service
public class ProcessDefinitionService {

    @Autowired private ProcessDefinitionRepository definitionRepository;
    @Autowired private ProcessInstanceRepository instanceRepository;
    @Autowired private ProcessValidationService validationService;

    // ── CRUD ─────────────────────────────────────────────────────────────────

    /**
     * Creates the process definition for an app.
     * Each app may have only one active (DRAFT or PUBLISHED) process at a time.
     * The processSlug uniquely identifies the process within the app.
     */
    public ProcessDefinition createProcess(String domainId, String appId, String processSlug,
            CreateProcessRequest request, String createdBy) {

        // Find the latest existing version for this process slug (if any)
        Optional<ProcessDefinition> latest = definitionRepository
                .findTopByDomainIdAndAppIdAndSlugOrderByVersionDesc(domainId, appId, processSlug);

        // Reject if an active (DRAFT or PUBLISHED) definition already exists
        if (latest.filter(d -> d.getStatus() == ProcessStatus.DRAFT
                            || d.getStatus() == ProcessStatus.PUBLISHED).isPresent()) {
            throw new IllegalStateException(
                    "This application already has a process definition. Update the existing one.");
        }

        // Next version = latest version + 1 (avoids duplicate key on archived records)
        int nextVersion = latest.map(d -> d.getVersion() + 1).orElse(1);

        ProcessDefinition def = new ProcessDefinition();
        def.setDomainId(domainId);
        def.setAppId(appId);
        def.setName(request.getName() != null ? request.getName() : processSlug);
        def.setSlug(processSlug);
        def.setDescription(request.getDescription());
        def.setNodes(request.getNodes());
        def.setEdges(request.getEdges());
        def.setSettings(request.getSettings() != null ? request.getSettings() : def.getSettings());
        def.setLinkedModelIds(request.getLinkedModelIds());
        def.setVersion(nextVersion);
        def.setStatus(ProcessStatus.DRAFT);
        def.setCreatedBy(createdBy);
        def.setCreatedAt(Instant.now());
        def.setUpdatedAt(Instant.now());

        return definitionRepository.save(def);
    }

    /**
     * Updates the DRAFT process definition for the app.
     * The processSlug is the lookup key.
     */
    public ProcessDefinition updateProcess(String domainId, String appId,
            String processSlug, UpdateProcessRequest request) {
        ProcessDefinition def = requireDraft(domainId, appId, processSlug);

        if (request.getName() != null && !request.getName().isBlank()) {
            def.setName(request.getName());
        }
        if (request.getDescription() != null) {
            def.setDescription(request.getDescription());
        }
        if (request.getNodes() != null) {
            def.setNodes(request.getNodes());
        }
        if (request.getEdges() != null) {
            def.setEdges(request.getEdges());
        }
        if (request.getSettings() != null) {
            def.setSettings(request.getSettings());
        }
        if (request.getLinkedModelIds() != null) {
            def.setLinkedModelIds(request.getLinkedModelIds());
        }
        def.setUpdatedAt(Instant.now());

        return definitionRepository.save(def);
    }

    /**
     * Validates and publishes the DRAFT definition.
     * Archives any currently published version for this slug first.
     */
    public ProcessDefinition publishProcess(String domainId, String appId, String processSlug) {
        ProcessDefinition draft = requireDraft(domainId, appId, processSlug);

        ValidationResult result = validationService.validate(draft);
        if (!result.isValid()) {
            throw new ProcessValidationException(result.getErrors());
        }

        // Archive the currently published version of this specific process if it exists
        definitionRepository.findByDomainIdAndAppIdAndSlug(domainId, appId, processSlug).stream()
                .filter(d -> d.getStatus() == ProcessStatus.PUBLISHED)
                .forEach(old -> {
                    old.setStatus(ProcessStatus.ARCHIVED);
                    old.setUpdatedAt(Instant.now());
                    definitionRepository.save(old);
                });

        draft.setStatus(ProcessStatus.PUBLISHED);
        draft.setUpdatedAt(Instant.now());
        return definitionRepository.save(draft);
    }

    /**
     * Archives the PUBLISHED process definition.
     */
    public ProcessDefinition archiveProcess(String domainId, String appId, String processSlug) {
        ProcessDefinition def = definitionRepository.findByDomainIdAndAppIdAndSlug(domainId, appId, processSlug).stream()
                .filter(d -> d.getStatus() == ProcessStatus.PUBLISHED)
                .findFirst()
                .orElseThrow(() -> new ProcessNotFoundException(
                        "No published process found for this application"));

        long activeCount = instanceRepository.countByProcessDefinitionIdAndStatus(def.getId(), InstanceStatus.ACTIVE);
        // Non-blocking: active instances continue running against their snapshotted version

        def.setStatus(ProcessStatus.ARCHIVED);
        def.setUpdatedAt(Instant.now());
        return definitionRepository.save(def);
    }

    /**
     * Returns the latest version of EACH process slug in this application.
     */
    public java.util.List<ProcessDefinition> getAllLatestProcesses(String domainId, String appId) {
        java.util.List<ProcessDefinition> all = definitionRepository.findByDomainIdAndAppId(domainId, appId);
        return all.stream()
                .collect(java.util.stream.Collectors.toMap(
                        ProcessDefinition::getSlug,
                        d -> d,
                        (d1, d2) -> d1.getVersion() > d2.getVersion() ? d1 : d2
                ))
                .values().stream()
                .toList();
    }

    /**
     * Returns the latest version of the app's process definition for a specific slug (any status).
     */
    public ProcessDefinition getProcess(String domainId, String appId, String processSlug) {
        return definitionRepository
                .findTopByDomainIdAndAppIdAndSlugOrderByVersionDesc(domainId, appId, processSlug)
                .orElseThrow(() -> new ProcessNotFoundException(
                        "No process definition found for this application"));
    }

    /** Hard-deletes the DRAFT process definition. */
    public void deleteProcess(String domainId, String appId, String processSlug) {
        ProcessDefinition def = requireDraft(domainId, appId, processSlug);
        definitionRepository.delete(def);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private ProcessDefinition requireDraft(String domainId, String appId, String processSlug) {
        ProcessDefinition def = definitionRepository
                .findTopByDomainIdAndAppIdAndSlugOrderByVersionDesc(domainId, appId, processSlug)
                .orElseThrow(() -> new ProcessNotFoundException(
                        "No process definition found for this application"));

        if (def.getStatus() != ProcessStatus.DRAFT) {
            throw new IllegalStateException(
                    "The process is " + def.getStatus() + " and cannot be modified directly. "
                            + "Archive it first, then create a new version.");
        }
        return def;
    }
}
