package com.adaptivebp.modules.process.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.adaptivebp.modules.process.dto.NodeViewResponse;
import com.adaptivebp.modules.process.dto.ProcessInstanceResponse;
import com.adaptivebp.modules.process.exception.InvalidNodeSubmissionException;
import com.adaptivebp.modules.process.exception.InsufficientProcessPermissionException;
import com.adaptivebp.modules.process.exception.ProcessAlreadyCompletedException;
import com.adaptivebp.modules.process.exception.ProcessNotFoundException;
import com.adaptivebp.modules.process.model.ProcessDefinition;
import com.adaptivebp.modules.process.model.ProcessEdge;
import com.adaptivebp.modules.process.model.ProcessInstance;
import com.adaptivebp.modules.process.model.ProcessNode;
import com.adaptivebp.modules.process.model.embedded.Assignment;
import com.adaptivebp.modules.process.model.embedded.CreatedRecord;
import com.adaptivebp.modules.process.model.embedded.HistoryEntry;
import com.adaptivebp.modules.process.model.enums.InstanceStatus;
import com.adaptivebp.modules.process.model.enums.NodeType;
import com.adaptivebp.modules.process.model.enums.ProcessStatus;
import com.adaptivebp.modules.formbuilder.model.ModelRecord;
import com.adaptivebp.modules.formbuilder.port.ModelRecordQueryPort;
import com.adaptivebp.modules.process.repository.ProcessDefinitionRepository;
import com.adaptivebp.modules.process.repository.ProcessInstanceRepository;

@Service
public class ProcessEngineService {

    private static final Logger log = LoggerFactory.getLogger(ProcessEngineService.class);
    private static final int MAX_AUTO_ADVANCE_HOPS = 50;

    @Autowired
    private ProcessDefinitionRepository definitionRepository;

    @Autowired
    private ProcessInstanceRepository instanceRepository;

    @Autowired
    private FormValidationService formValidationService;

    @Autowired
    private ModelRecordQueryPort modelRecordQueryPort;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Starts a new process instance from a PUBLISHED definition.
     * Auto-advances past START to the first interactive node.
     */
    public ProcessInstanceResponse startProcess(String domainId, String appId, String userId) {
        // Each app has exactly one process — find the PUBLISHED one
        ProcessDefinition def = definitionRepository
                .findByDomainIdAndAppIdAndStatus(domainId, appId, ProcessStatus.PUBLISHED)
                .stream().findFirst()
                .orElseThrow(() -> new ProcessNotFoundException(
                        "This application has no published workflow. Contact your administrator to publish the process first."));

        ProcessNode startNode = def.findStartNode();
        if (startNode == null) {
            throw new ProcessNotFoundException("Process definition has no START node");
        }

        ProcessInstance instance = new ProcessInstance();
        instance.setProcessDefinitionId(def.getId());
        instance.setProcessVersion(def.getVersion());
        instance.setDomainId(domainId);
        instance.setAppId(appId);
        instance.setStatus(InstanceStatus.ACTIVE);
        instance.setCurrentNodeId(startNode.getId());
        instance.setStartedBy(userId);
        instance.setStartedAt(Instant.now());

        appendHistory(instance, startNode.getId(), "ENTERED", userId, null, null);

        // Auto-advance past START to the first interactive node
        advanceToNext(instance, def, startNode.getId(), null, userId, 0);

        ProcessInstance saved = instanceRepository.save(instance);
        ProcessNode currentNode = def.findNodeById(saved.getCurrentNodeId());
        return ProcessInstanceResponse.of(saved, currentNode);
    }

    /**
     * Submits data for the current node and advances the process.
     */
    public ProcessInstanceResponse submitNode(String instanceId, String nodeId,
            Map<String, Object> formData, String action, String comment, String userId) {
        ProcessInstance instance = requireActiveInstance(instanceId);
        ProcessDefinition def = loadDefinitionForInstance(instance);

        // 1 — Verify frontend is in sync
        if (!nodeId.equals(instance.getCurrentNodeId())) {
            throw new InvalidNodeSubmissionException(
                    "Node mismatch: expected '" + instance.getCurrentNodeId() + "' but got '" + nodeId + "'");
        }

        ProcessNode node = def.findNodeById(nodeId);
        if (node == null) {
            throw new InvalidNodeSubmissionException("Node '" + nodeId + "' not found in definition");
        }

        // 2 — Check node-level permission
        checkNodePermission(node, userId);

        // 3 — Handle by node type
        switch (node.getType()) {
            case FORM_PAGE -> {
                List<FormValidationService.FieldError> fieldErrors =
                        formValidationService.validate(node.getConfig(), formData != null ? formData : Map.of());
                if (!fieldErrors.isEmpty()) {
                    List<String> messages = fieldErrors.stream()
                            .map(e -> e.elementId() + ": " + e.message()).toList();
                    throw new InvalidNodeSubmissionException("Form validation failed: " + String.join("; ", messages));
                }
                // Merge into instance.data with "nodeId__elementId" key format (no dots — MongoDB restriction)
                if (formData != null) {
                    for (Map.Entry<String, Object> entry : formData.entrySet()) {
                        instance.getData().put(nodeId + "__" + entry.getKey(), entry.getValue());
                    }
                }
                appendHistory(instance, nodeId, "SUBMITTED", userId, formData, comment);
            }
            case APPROVAL -> {
                if (action == null || action.isBlank()) {
                    throw new InvalidNodeSubmissionException("APPROVAL node requires an action (approve/reject)");
                }
                // Merge any approval form data
                if (formData != null) {
                    for (Map.Entry<String, Object> entry : formData.entrySet()) {
                        instance.getData().put(nodeId + "__" + entry.getKey(), entry.getValue());
                    }
                }
                instance.getData().put(nodeId + "___action", action);
                appendHistory(instance, nodeId, action.toUpperCase(), userId, formData, comment);
            }
            case DATA_VIEW -> {
                // User just viewed the data — no input to collect
                appendHistory(instance, nodeId, "VIEWED", userId, null, comment);
            }
            default -> throw new InvalidNodeSubmissionException(
                    "Node type " + node.getType() + " cannot be submitted directly");
        }

        // 4 — Advance to the next node
        advanceToNext(instance, def, nodeId, action, userId, 0);

        ProcessInstance saved = instanceRepository.save(instance);
        ProcessNode currentNode = def.findNodeById(saved.getCurrentNodeId());
        return ProcessInstanceResponse.of(saved, currentNode);
    }

    /**
     * Returns what the current node should render for the user.
     */
    public NodeViewResponse getNodeView(String instanceId, String userId) {
        ProcessInstance instance = requireActiveInstance(instanceId);
        ProcessDefinition def = loadDefinitionForInstance(instance);
        ProcessNode node = def.findNodeById(instance.getCurrentNodeId());
        if (node == null) {
            throw new ProcessNotFoundException("Current node not found in definition");
        }

        NodeViewResponse view = new NodeViewResponse();
        view.setNodeId(node.getId());
        view.setNodeName(node.getName());
        view.setNodeType(node.getType());
        view.setConfig(node.getConfig() != null ? node.getConfig() : new HashMap<>());

        switch (node.getType()) {
            case FORM_PAGE -> {
                // Return any draft or previously saved data for this node
                Map<String, Object> prefilled = new HashMap<>();
                String prefix = node.getId() + "__";
                for (Map.Entry<String, Object> entry : instance.getData().entrySet()) {
                    if (entry.getKey().startsWith(prefix)) {
                        prefilled.put(entry.getKey().substring(prefix.length()), entry.getValue());
                    }
                }
                // Overlay with draft data (draft takes precedence)
                for (Map.Entry<String, Object> entry : instance.getDraftData().entrySet()) {
                    if (entry.getKey().startsWith(prefix)) {
                        prefilled.put(entry.getKey().substring(prefix.length()), entry.getValue());
                    }
                }
                view.setPrefilledData(prefilled);
                view.setAvailableActions(List.of("submit"));
            }
            case APPROVAL -> {
                view.setAvailableActions(extractApprovalActions(node));
                // Provide a summary of instance data for context
                view.setPrefilledData(new HashMap<>(instance.getData()));
            }
            case DATA_VIEW -> {
                String modelId = (String) node.getConfig().get("modelId");
                if (modelId != null && !modelId.isBlank()) {
                    @SuppressWarnings("unchecked")
                    List<String> displayFields = (List<String>) node.getConfig().get("displayFields");
                    List<ModelRecord> records = modelRecordQueryPort.findByModel(modelId, instance.getDomainId());
                    List<Map<String, Object>> recordData = records.stream()
                            .map(r -> {
                                Map<String, Object> row = new java.util.LinkedHashMap<>();
                                row.put("_id", r.getId());
                                if (displayFields == null || displayFields.isEmpty()) {
                                    row.putAll(r.getData());
                                } else {
                                    for (String field : displayFields) {
                                        row.put(field, r.getData().get(field));
                                    }
                                }
                                return row;
                            })
                            .toList();
                    view.setRecords(recordData);
                } else {
                    view.setRecords(List.of());
                }
                view.setAvailableActions(List.of("next"));
            }
            case END -> {
                view.setCompletionMessage("Process completed successfully");
                view.setAvailableActions(List.of());
            }
            default -> { /* no special rendering */ }
        }

        return view;
    }

    /**
     * Saves partial form data without advancing the process.
     */
    public ProcessInstance saveDraft(String instanceId, String nodeId,
            Map<String, Object> partialData, String userId) {
        ProcessInstance instance = requireActiveInstance(instanceId);

        if (!nodeId.equals(instance.getCurrentNodeId())) {
            throw new InvalidNodeSubmissionException(
                    "Node mismatch: cannot save draft for node '" + nodeId + "'");
        }

        if (partialData != null) {
            String prefix = nodeId + "__";
            for (Map.Entry<String, Object> entry : partialData.entrySet()) {
                instance.getDraftData().put(prefix + entry.getKey(), entry.getValue());
            }
        }

        return instanceRepository.save(instance);
    }

    /**
     * Cancels an active instance.
     */
    public ProcessInstance cancelInstance(String instanceId, String userId) {
        ProcessInstance instance = requireActiveInstance(instanceId);
        instance.setStatus(InstanceStatus.CANCELLED);
        appendHistory(instance, instance.getCurrentNodeId(), "CANCELLED", userId, null, null);
        return instanceRepository.save(instance);
    }

    public ProcessInstance getInstance(String instanceId) {
        return instanceRepository.findById(instanceId)
                .orElseThrow(() -> new ProcessNotFoundException("Instance not found: " + instanceId));
    }

    public List<ProcessInstance> listInstances(String domainId, String appId) {
        return instanceRepository.findByDomainIdAndAppId(domainId, appId);
    }

    public List<ProcessInstance> getMyTasks(String userId) {
        return instanceRepository.findByAssignedToUserIdAndStatus(userId, InstanceStatus.ACTIVE);
    }

    // ── Core engine — private ─────────────────────────────────────────────────

    /**
     * Recursively advances the instance through silent nodes
     * (CONDITION, DATA_ACTION, NOTIFICATION) until it hits a node
     * that requires human interaction or reaches END.
     * Safety limit: MAX_AUTO_ADVANCE_HOPS to prevent infinite loops.
     */
    private void advanceToNext(ProcessInstance instance, ProcessDefinition def,
            String fromNodeId, String lastAction, String userId, int hopCount) {
        if (hopCount >= MAX_AUTO_ADVANCE_HOPS) {
            log.error("Max auto-advance hops reached for instance {}. Pausing.", instance.getId());
            instance.setStatus(InstanceStatus.PAUSED);
            return;
        }

        List<ProcessEdge> outgoing = def.outgoingEdges(fromNodeId);
        if (outgoing.isEmpty()) {
            // No outgoing edges — should only happen at END (already handled below)
            return;
        }

        ProcessEdge chosenEdge = chooseEdge(outgoing, def, fromNodeId, instance, lastAction);
        if (chosenEdge == null) {
            log.warn("Could not resolve outgoing edge from node '{}'. Pausing instance {}.",
                    fromNodeId, instance.getId());
            instance.setStatus(InstanceStatus.PAUSED);
            return;
        }

        ProcessNode targetNode = def.findNodeById(chosenEdge.getToNodeId());
        if (targetNode == null) {
            log.error("Edge '{}' points to non-existent node '{}'. Pausing instance {}.",
                    chosenEdge.getId(), chosenEdge.getToNodeId(), instance.getId());
            instance.setStatus(InstanceStatus.PAUSED);
            return;
        }

        instance.setPreviousNodeId(instance.getCurrentNodeId());
        instance.setCurrentNodeId(targetNode.getId());

        switch (targetNode.getType()) {
            case CONDITION -> {
                appendHistory(instance, targetNode.getId(), "AUTO_ROUTED", userId, null, null);
                advanceToNext(instance, def, targetNode.getId(), null, userId, hopCount + 1);
            }
            case DATA_ACTION -> {
                boolean success = executeDataAction(instance, targetNode, userId);
                if (!success) {
                    // Roll back to previous node on failure
                    instance.setCurrentNodeId(instance.getPreviousNodeId());
                    instance.setStatus(InstanceStatus.PAUSED);
                    return;
                }
                advanceToNext(instance, def, targetNode.getId(), null, userId, hopCount + 1);
            }
            case NOTIFICATION -> {
                // Phase 1: log only — wire up real notifications in Phase 2
                log.info("[NOTIFICATION] Instance {} would send notification via node '{}'",
                        instance.getId(), targetNode.getId());
                appendHistory(instance, targetNode.getId(), "AUTO_ROUTED", userId, null, "Notification skipped (Phase 2)");
                advanceToNext(instance, def, targetNode.getId(), null, userId, hopCount + 1);
            }
            case END -> {
                instance.setStatus(InstanceStatus.COMPLETED);
                instance.setCompletedAt(Instant.now());
                appendHistory(instance, targetNode.getId(), "COMPLETED", userId, null, null);
            }
            case START -> {
                // Should never land back on START — treat as stuck
                log.error("advanceToNext landed on START node. Pausing instance {}.", instance.getId());
                instance.setStatus(InstanceStatus.PAUSED);
            }
            default -> {
                // FORM_PAGE, APPROVAL, DATA_VIEW — stop here, needs user interaction
                appendHistory(instance, targetNode.getId(), "ENTERED", userId, null, null);
            }
        }
    }

    /**
     * Selects the outgoing edge to follow based on node type and context.
     */
    @SuppressWarnings("unchecked")
    private ProcessEdge chooseEdge(List<ProcessEdge> outgoing, ProcessDefinition def,
            String fromNodeId, ProcessInstance instance, String lastAction) {
        if (outgoing.size() == 1) return outgoing.get(0);

        ProcessNode fromNode = def.findNodeById(fromNodeId);
        if (fromNode == null) return null;

        if (fromNode.getType() == NodeType.CONDITION) {
            return evaluateConditionEdge(fromNode, outgoing, instance);
        }

        if (fromNode.getType() == NodeType.APPROVAL && lastAction != null) {
            // Match edge whose conditionRef equals the action taken
            for (ProcessEdge edge : outgoing) {
                if (lastAction.equalsIgnoreCase(edge.getConditionRef())) return edge;
            }
            // Fallback: first edge
            return outgoing.get(0);
        }

        return outgoing.get(0);
    }

    @SuppressWarnings("unchecked")
    private ProcessEdge evaluateConditionEdge(ProcessNode conditionNode,
            List<ProcessEdge> outgoing, ProcessInstance instance) {
        Map<String, Object> config = conditionNode.getConfig();
        if (config == null) return outgoing.get(0);

        List<Map<String, Object>> rules = (List<Map<String, Object>>) config.get("rules");
        String defaultEdgeId = (String) config.get("defaultEdgeId");

        if (rules != null) {
            for (Map<String, Object> rule : rules) {
                List<Map<String, Object>> conditions = (List<Map<String, Object>>) rule.get("conditions");
                if (conditions != null && allConditionsMatch(conditions, instance.getData())) {
                    String targetEdgeId = (String) rule.get("targetEdgeId");
                    return findEdgeById(outgoing, targetEdgeId);
                }
            }
        }

        // No rule matched — use default
        return findEdgeById(outgoing, defaultEdgeId);
    }

    @SuppressWarnings("unchecked")
    private boolean allConditionsMatch(List<Map<String, Object>> conditions, Map<String, Object> data) {
        for (Map<String, Object> condition : conditions) {
            String field = (String) condition.get("field");
            String operator = (String) condition.get("operator");
            Object expected = condition.get("value");
            Object actual = data.get(field);
            if (!evaluateOperator(actual, operator, expected)) return false;
        }
        return true;
    }

    private boolean evaluateOperator(Object actual, String operator, Object expected) {
        String actualStr = actual == null ? "" : actual.toString();
        String expectedStr = expected == null ? "" : expected.toString();
        return switch (operator) {
            case "EQUALS" -> actualStr.equals(expectedStr);
            case "NOT_EQUALS" -> !actualStr.equals(expectedStr);
            case "IS_EMPTY" -> actualStr.isBlank();
            case "IS_NOT_EMPTY" -> !actualStr.isBlank();
            case "CONTAINS" -> actualStr.contains(expectedStr);
            case "GREATER_THAN" -> {
                try { yield Double.parseDouble(actualStr) > Double.parseDouble(expectedStr); }
                catch (NumberFormatException e) { yield false; }
            }
            case "LESS_THAN" -> {
                try { yield Double.parseDouble(actualStr) < Double.parseDouble(expectedStr); }
                catch (NumberFormatException e) { yield false; }
            }
            default -> false;
        };
    }

    private ProcessEdge findEdgeById(List<ProcessEdge> edges, String edgeId) {
        if (edgeId == null) return edges.isEmpty() ? null : edges.get(0);
        return edges.stream().filter(e -> edgeId.equals(e.getId())).findFirst().orElse(edges.get(0));
    }

    /**
     * Executes a DATA_ACTION node: reads config, resolves field mappings,
     * performs CREATE/UPDATE/DELETE on the model's data collection.
     * Returns true on success, false on failure.
     */
    @SuppressWarnings("unchecked")
    private boolean executeDataAction(ProcessInstance instance, ProcessNode node, String userId) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            log.error("DATA_ACTION node '{}' has no config", node.getId());
            return false;
        }

        String operation = (String) config.get("operation");
        String modelId = (String) config.get("modelId");
        List<Map<String, Object>> fieldMappings =
                (List<Map<String, Object>>) config.get("fieldMappings");

        if (operation == null || modelId == null) {
            log.error("DATA_ACTION node '{}' missing operation or modelId", node.getId());
            return false;
        }

        // Resolve field values from mappings
        Map<String, Object> record = new HashMap<>();
        if (fieldMappings != null) {
            for (Map<String, Object> mapping : fieldMappings) {
                String targetField = (String) mapping.get("targetField");
                String source = (String) mapping.get("source");
                String value = (String) mapping.get("value");

                Object resolvedValue = resolveFieldValue(source, value, instance, userId);
                if (targetField != null) {
                    record.put(targetField, resolvedValue);
                }
            }
        }

        try {
            switch (operation.toUpperCase()) {
                case "CREATE" -> {
                    ModelRecord saved = modelRecordQueryPort.create(
                            modelId, instance.getDomainId(), instance.getAppId(),
                            instance.getId(), userId, record);
                    CreatedRecord created = new CreatedRecord();
                    created.setModelId(modelId);
                    created.setRecordId(saved.getId());
                    created.setCreatedAt(Instant.now());
                    instance.getCreatedRecordIds().add(created);
                    instance.getData().put(node.getId() + "___createdRecordId", saved.getId());
                    appendHistory(instance, node.getId(), "DATA_ACTION_EXECUTED", userId,
                            Map.of("operation", "CREATE", "modelId", modelId, "recordId", saved.getId()), null);
                }
                case "UPDATE" -> {
                    // Look up the recordId stored by a previous CREATE node
                    String recordId = resolveRecordId(config, instance);
                    if (recordId != null) {
                        modelRecordQueryPort.update(recordId, record);
                    } else {
                        log.warn("DATA_ACTION UPDATE: no recordId resolved for node '{}'. Skipping.", node.getId());
                    }
                    appendHistory(instance, node.getId(), "DATA_ACTION_EXECUTED", userId,
                            Map.of("operation", "UPDATE", "modelId", modelId, "record", record), null);
                }
                case "DELETE" -> {
                    String recordId = resolveRecordId(config, instance);
                    if (recordId != null) {
                        modelRecordQueryPort.delete(recordId);
                    } else {
                        log.warn("DATA_ACTION DELETE: no recordId resolved for node '{}'. Skipping.", node.getId());
                    }
                    appendHistory(instance, node.getId(), "DATA_ACTION_EXECUTED", userId,
                            Map.of("operation", "DELETE", "modelId", modelId), null);
                }
                default -> {
                    log.error("Unknown DATA_ACTION operation: {}", operation);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("DATA_ACTION execution failed for node '{}': {}", node.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Resolves the recordId for UPDATE/DELETE operations.
     * Checks: 1) explicit "recordIdRef" in config (points to a key in instance.data),
     *         2) the most recent createdRecordId for the same modelId.
     */
    private String resolveRecordId(Map<String, Object> config, ProcessInstance instance) {
        String ref = (String) config.get("recordIdRef");
        if (ref != null) {
            Object val = instance.getData().get(ref);
            if (val != null) return val.toString();
        }
        // Fallback: last created record for this modelId
        String modelId = (String) config.get("modelId");
        if (modelId != null) {
            return instance.getCreatedRecordIds().stream()
                    .filter(cr -> modelId.equals(cr.getModelId()))
                    .reduce((first, second) -> second) // last one
                    .map(cr -> cr.getRecordId())
                    .orElse(null);
        }
        return null;
    }

    private Object resolveFieldValue(String source, String value,
            ProcessInstance instance, String userId) {
        if (source == null) return value;
        return switch (source) {
            case "FORM_FIELD" -> instance.getData().get(value);
            case "STATIC" -> value;
            case "CONTEXT" -> resolveContext(value, instance, userId);
            default -> value;
        };
    }

    private Object resolveContext(String key, ProcessInstance instance, String userId) {
        return switch (key) {
            case "currentUserId" -> userId;
            case "currentDate" -> Instant.now().toString().substring(0, 10);
            case "currentDateTime" -> Instant.now().toString();
            case "processInstanceId" -> instance.getId();
            default -> null;
        };
    }

    private void checkNodePermission(ProcessNode node, String userId) {
        // If no allowedRoles or allowedUserIds specified, node is open to all authenticated users
        if (node.getPermissions() == null) return;
        List<String> allowedUsers = node.getPermissions().getAllowedUserIds();
        if (allowedUsers != null && !allowedUsers.isEmpty() && !allowedUsers.contains(userId)) {
            // Check roles via allowedRoles — for now, owner-level check is handled at controller
            // Fine-grained role check requires role resolution integration (Phase 2)
            // For Phase 1: userId-based restriction only
            if (node.getPermissions().getAllowedRoles() == null
                    || node.getPermissions().getAllowedRoles().isEmpty()) {
                throw new InsufficientProcessPermissionException(
                        "You do not have permission to interact with node '" + node.getId() + "'");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractApprovalActions(ProcessNode node) {
        List<String> actions = new ArrayList<>();
        if (node.getConfig() == null) return List.of("approve", "reject");
        List<Map<String, Object>> configActions = (List<Map<String, Object>>) node.getConfig().get("actions");
        if (configActions != null) {
            for (Map<String, Object> a : configActions) {
                Object id = a.get("id");
                if (id != null) actions.add(id.toString());
            }
        }
        return actions.isEmpty() ? List.of("approve", "reject") : actions;
    }

    private void appendHistory(ProcessInstance instance, String nodeId, String action,
            String userId, Map<String, Object> data, String comment) {
        HistoryEntry entry = new HistoryEntry();
        entry.setNodeId(nodeId);
        entry.setAction(action);
        entry.setPerformedBy(userId);
        entry.setPerformedAt(Instant.now());
        if (data != null) entry.setData(data);
        entry.setComment(comment);
        instance.appendHistory(entry);
    }

    private ProcessInstance requireActiveInstance(String instanceId) {
        ProcessInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new ProcessNotFoundException("Instance not found: " + instanceId));
        if (instance.getStatus() != InstanceStatus.ACTIVE) {
            throw new ProcessAlreadyCompletedException(instanceId);
        }
        return instance;
    }

    private ProcessDefinition loadDefinitionForInstance(ProcessInstance instance) {
        return definitionRepository.findById(instance.getProcessDefinitionId())
                .orElseThrow(() -> new ProcessNotFoundException(
                        "Definition not found for instance " + instance.getId()));
    }
}
