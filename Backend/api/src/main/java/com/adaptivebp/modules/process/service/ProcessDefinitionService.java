package com.adaptivebp.modules.process.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.adaptivebp.modules.process.dto.CreateProcessRequest;
import com.adaptivebp.modules.process.dto.UpdateProcessRequest;
import com.adaptivebp.modules.process.dto.ValidationResult;
import com.adaptivebp.modules.process.exception.ProcessNotFoundException;
import com.adaptivebp.modules.process.exception.ProcessValidationException;
import com.adaptivebp.modules.process.model.ProcessDefinition;
import com.adaptivebp.modules.process.model.ProcessEdge;
import com.adaptivebp.modules.process.model.ProcessNode;
import com.adaptivebp.modules.process.model.ProcessTemplate;
import com.adaptivebp.modules.process.model.enums.InstanceStatus;
import com.adaptivebp.modules.process.model.enums.ProcessStatus;
import com.adaptivebp.modules.process.repository.ProcessDefinitionRepository;
import com.adaptivebp.modules.process.repository.ProcessInstanceRepository;

/**
 * Each application has exactly one process definition.
 * The appSlug is used as the process slug — no independent slug management.
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
     * The appSlug is used as the process slug.
     */
    public ProcessDefinition createProcess(String domainId, String appId, String appSlug,
            CreateProcessRequest request, String createdBy) {

        // Find the latest existing version (if any)
        Optional<ProcessDefinition> latest = definitionRepository
                .findTopByDomainIdAndAppIdOrderByVersionDesc(domainId, appId);

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
        def.setName(request.getName() != null ? request.getName() : appSlug);
        def.setSlug(appSlug);
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
     * The appSlug is the lookup key.
     */
    public ProcessDefinition updateProcess(String domainId, String appId,
            String appSlug, UpdateProcessRequest request) {
        ProcessDefinition def = requireDraft(domainId, appId);

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
     * Archives any currently published version first.
     */
    public ProcessDefinition publishProcess(String domainId, String appId, String appSlug) {
        ProcessDefinition draft = requireDraft(domainId, appId);

        ValidationResult result = validationService.validate(draft);
        if (!result.isValid()) {
            throw new ProcessValidationException(result.getErrors());
        }

        // Archive the currently published version if it exists
        definitionRepository.findByDomainIdAndAppId(domainId, appId).stream()
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
    public ProcessDefinition archiveProcess(String domainId, String appId, String appSlug) {
        ProcessDefinition def = definitionRepository.findByDomainIdAndAppId(domainId, appId).stream()
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
     * Returns the latest version of the app's process definition (any status).
     * appSlug is used as the process slug.
     */
    public ProcessDefinition getProcess(String domainId, String appId, String appSlug) {
        return definitionRepository
                .findTopByDomainIdAndAppIdOrderByVersionDesc(domainId, appId)
                .orElseThrow(() -> new ProcessNotFoundException(
                        "No process definition found for this application"));
    }

    /** Hard-deletes the DRAFT process definition. */
    public void deleteProcess(String domainId, String appId, String appSlug) {
        ProcessDefinition def = requireDraft(domainId, appId);
        definitionRepository.delete(def);
    }

    /**
     * Creates a new process definition from a template.
     * The template's nodes and edges are copied to a new DRAFT process.
     */
    public ProcessDefinition createFromTemplate(String domainId, String appId, String appSlug,
            ProcessTemplate template, List<String> linkedModelIds, String createdBy) {

        // Check if there's already an active process for this app
        Optional<ProcessDefinition> existing = definitionRepository
                .findTopByDomainIdAndAppIdOrderByVersionDesc(domainId, appId);

        if (existing.filter(d -> d.getStatus() == ProcessStatus.DRAFT
                            || d.getStatus() == ProcessStatus.PUBLISHED).isPresent()) {
            throw new IllegalStateException(
                    "This application already has a process definition. Archive it first to use a template.");
        }

        // Calculate next version number
        int nextVersion = existing.map(d -> d.getVersion() + 1).orElse(1);

        // Create new process definition from template
        ProcessDefinition definition = new ProcessDefinition();
        definition.setDomainId(domainId);
        definition.setAppId(appId);
        definition.setSlug(appSlug);
        definition.setName(template.getName() + " (from template)");
        definition.setDescription(template.getDescription());
        definition.setVersion(nextVersion);
        definition.setStatus(ProcessStatus.DRAFT);
        definition.setCreatedBy(createdBy);
        definition.setCreatedAt(Instant.now());

        // Set linkedModelIds if provided from frontend (created models)
        if (linkedModelIds != null && !linkedModelIds.isEmpty()) {
            definition.setLinkedModelIds(new ArrayList<>(linkedModelIds));
        } else {
            definition.setLinkedModelIds(new ArrayList<>());
        }

        // Copy nodes from template
        List<ProcessNode> nodes = new ArrayList<>();
        if (template.getNodes() != null) {
            for (ProcessNode templateNode : template.getNodes()) {
                ProcessNode node = new ProcessNode();
                node.setId(templateNode.getId());
                node.setType(templateNode.getType());
                node.setName(templateNode.getName());
                node.setConfig(templateNode.getConfig() != null ?
                    new java.util.HashMap<>(templateNode.getConfig()) : new java.util.HashMap<>());
                nodes.add(node);
            }
        }
        definition.setNodes(nodes);

        // Copy edges from template
        List<ProcessEdge> edges = new ArrayList<>();
        if (template.getEdges() != null) {
            for (ProcessEdge templateEdge : template.getEdges()) {
                ProcessEdge edge = new ProcessEdge();
                edge.setId(templateEdge.getId());
                edge.setFromNodeId(templateEdge.getFromNodeId());
                edge.setToNodeId(templateEdge.getToNodeId());
                edge.setLabel(templateEdge.getLabel());
                edge.setConditionRef(templateEdge.getConditionRef());
                edges.add(edge);
            }
        }
        definition.setEdges(edges);

        return definitionRepository.save(definition);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private ProcessDefinition requireDraft(String domainId, String appId) {
        ProcessDefinition def = definitionRepository
                .findTopByDomainIdAndAppIdOrderByVersionDesc(domainId, appId)
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
