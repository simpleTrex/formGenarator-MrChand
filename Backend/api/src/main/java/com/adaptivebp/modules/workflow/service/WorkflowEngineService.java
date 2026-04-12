package com.adaptivebp.modules.workflow.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.adaptivebp.modules.appmanagement.model.AppGroup;
import com.adaptivebp.modules.appmanagement.model.AppGroupMember;
import com.adaptivebp.modules.appmanagement.permission.AppPermission;
import com.adaptivebp.modules.appmanagement.repository.AppGroupMemberRepository;
import com.adaptivebp.modules.appmanagement.repository.AppGroupRepository;
import com.adaptivebp.modules.organisation.model.DomainGroup;
import com.adaptivebp.modules.organisation.model.DomainGroupMember;
import com.adaptivebp.modules.organisation.model.enums.DomainGroupType;
import com.adaptivebp.modules.organisation.repository.DomainGroupMemberRepository;
import com.adaptivebp.modules.organisation.repository.DomainGroupRepository;
import com.adaptivebp.modules.formbuilder.model.DomainFieldType;
import com.adaptivebp.modules.formbuilder.model.DomainModelField;
import com.adaptivebp.modules.workflow.dto.response.ExecuteEdgeResponse;
import com.adaptivebp.modules.workflow.dto.response.HistoryResponse;
import com.adaptivebp.modules.workflow.dto.response.StepViewResponse;
import com.adaptivebp.modules.workflow.dto.response.TaskListResponse;
import com.adaptivebp.modules.workflow.dto.response.TaskResponse;
import com.adaptivebp.modules.workflow.exception.ConditionNotMetException;
import com.adaptivebp.modules.workflow.exception.EdgeNotFoundException;
import com.adaptivebp.modules.workflow.exception.InsufficientEdgePermissionException;
import com.adaptivebp.modules.workflow.exception.InvalidFormDataException;
import com.adaptivebp.modules.workflow.exception.WorkflowAlreadyCompletedException;
import com.adaptivebp.modules.workflow.exception.WorkflowNotFoundException;
import com.adaptivebp.modules.workflow.model.AutoAction;
import com.adaptivebp.modules.workflow.model.EdgeCondition;
import com.adaptivebp.modules.workflow.model.InstanceHistory;
import com.adaptivebp.modules.workflow.model.WorkflowDefinition;
import com.adaptivebp.modules.workflow.model.WorkflowEdge;
import com.adaptivebp.modules.workflow.model.WorkflowInstance;
import com.adaptivebp.modules.workflow.model.WorkflowStep;
import com.adaptivebp.modules.workflow.model.enums.ConditionOperator;
import com.adaptivebp.modules.workflow.model.enums.InstanceStatus;
import com.adaptivebp.modules.workflow.model.enums.WorkflowStatus;
import com.adaptivebp.modules.workflow.repository.WorkflowDefinitionRepository;
import com.adaptivebp.modules.workflow.repository.WorkflowInstanceRepository;

@Service
public class WorkflowEngineService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngineService.class);

    @Autowired
    private WorkflowInstanceRepository instanceRepository;

    @Autowired
    private WorkflowDefinitionRepository definitionRepository;

    @Autowired
    private AppGroupMemberRepository appGroupMemberRepository;

    @Autowired
    private AppGroupRepository appGroupRepository;

    @Autowired
    private DomainGroupMemberRepository domainGroupMemberRepository;

    @Autowired
    private DomainGroupRepository domainGroupRepository;

    public WorkflowInstance startWorkflowBySlug(
            String domainId,
            String appId,
            String workflowSlug,
            Map<String, Object> formData,
            String userId,
            String performedByName) {
        WorkflowDefinition definition = definitionRepository
                .findByDomainIdAndAppIdAndSlugAndStatus(domainId, appId, workflowSlug, WorkflowStatus.PUBLISHED)
                .orElseThrow(() -> new WorkflowNotFoundException("Published workflow not found: " + workflowSlug));

        return startWorkflow(definition.getId(), formData, userId, performedByName);
    }

    public WorkflowInstance startWorkflow(
            String definitionId,
            Map<String, Object> formData,
            String userId,
            String performedByName) {
        WorkflowDefinition definition = definitionRepository.findById(definitionId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow definition not found: " + definitionId));

        if (definition.getStatus() != WorkflowStatus.PUBLISHED) {
            throw new IllegalStateException("Workflow is not published");
        }

        WorkflowStep startStep = definition.findStartStep()
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow has no start step"));

        Map<String, Object> payload = formData != null ? new HashMap<>(formData) : new HashMap<>();
        if (!payload.isEmpty()) {
            Map<String, String> fieldErrors = validateStepData(startStep, payload, true);
            if (!fieldErrors.isEmpty()) {
                throw new InvalidFormDataException("Invalid start step form data", fieldErrors);
            }
        }

        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowDefinitionId(definition.getId());
        instance.setWorkflowVersion(definition.getVersion());
        instance.setDomainId(definition.getDomainId());
        instance.setAppId(definition.getAppId());
        instance.setStatus(InstanceStatus.ACTIVE);
        instance.setCurrentStepId(startStep.getId());
        instance.setStartedBy(userId);
        instance.setStartedAt(Instant.now());

        // Seed the primary record with any start-step form data
        if (!payload.isEmpty()) {
            instance.getPrimaryRecord().putAll(payload);
            putStepRecord(instance, startStep.getId(), new HashMap<>(payload));
        }

        InstanceHistory history = new InstanceHistory();
        history.setStepId(startStep.getId());
        history.setEdgeId(null);
        history.setEdgeName("Started");
        history.setPerformedBy(userId);
        history.setPerformedByName(performedByName);
        history.setPerformedAt(Instant.now());
        history.setRecordId(null);
        history.setFormData(new HashMap<>(payload));
        instance.getHistory().add(history);

        return instanceRepository.save(instance);
    }

    public ExecuteEdgeResponse executeEdge(
            String instanceId,
            String edgeId,
            Map<String, Object> formData,
            String comment,
            String userId,
            String performedByName) {
        WorkflowInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow instance not found: " + instanceId));

        if (instance.getStatus() != InstanceStatus.ACTIVE) {
            throw new WorkflowAlreadyCompletedException("Workflow instance is not ACTIVE");
        }

        WorkflowDefinition definition = definitionRepository.findById(instance.getWorkflowDefinitionId())
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow definition not found"));

        WorkflowStep currentStep = definition.findStepById(instance.getCurrentStepId());
        if (currentStep == null) {
            throw new WorkflowNotFoundException("Current step not found in definition: " + instance.getCurrentStepId());
        }

        WorkflowEdge edge = resolveEdge(definition, currentStep, edgeId);

        Set<AppPermission> userPermissions = loadUserPermissions(instance.getAppId(), userId);
        Set<String> userRoles = loadUserRoles(instance.getDomainId(), userId);
        boolean isSubmitter = userId != null && userId.equals(instance.getStartedBy());

        // Coarse gate: user must have app execute permission, OR workflow roles, OR be the submitter
        if (!canExecuteWorkflow(userPermissions) && userRoles.isEmpty() && !isSubmitter) {
            throw new InsufficientEdgePermissionException("You do not have permission to execute workflow actions");
        }

        // Fine-grained gate: check the specific edge's role/submitter/userId rules
        if (!hasEdgePermission(edge, userRoles, userId, instance.getStartedBy())) {
            throw new InsufficientEdgePermissionException(
                    "You do not have permission to execute '" + edge.getName() + "'");
        }

        Map<String, Object> payload = formData != null ? new HashMap<>(formData) : new HashMap<>();

        // Conditions are evaluated against the full primary record merged with the new payload
        Map<String, Object> mergedForConditions = new HashMap<>(instance.getPrimaryRecord());
        mergedForConditions.putAll(payload);

        if (!conditionsPass(edge.getConditions(), definition, instance, currentStep, mergedForConditions)) {
            throw new ConditionNotMetException("Conditions for edge '" + edge.getName() + "' are not met");
        }

        // Validate required fields on the step (only editable fields of this step)
        Map<String, Object> existingRecordData = getStepRecordMap(instance, currentStep.getId());
        boolean requireStepRequiredFields = existingRecordData == null || existingRecordData.isEmpty();
        Map<String, String> fieldErrors = validateStepData(currentStep, payload, requireStepRequiredFields);
        validateEdgeRequiredFields(edge, mergedForConditions, fieldErrors);
        if (!fieldErrors.isEmpty()) {
            throw new InvalidFormDataException("Invalid form data", fieldErrors);
        }

        // Merge submitted data into per-step record (for history/audit purposes)
        Map<String, Object> recordData = existingRecordData != null ? new HashMap<>(existingRecordData) : new HashMap<>();
        recordData.putAll(payload);
        putStepRecord(instance, currentStep.getId(), recordData);

        // Write ONLY the editable fields of this step into the primary record
        Set<String> editableKeys = new HashSet<>();
        if (currentStep.getFields() != null) {
            for (com.adaptivebp.modules.formbuilder.model.DomainModelField f : currentStep.getFields()) {
                editableKeys.add(f.getKey());
            }
        }
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (editableKeys.isEmpty() || editableKeys.contains(entry.getKey())) {
                instance.getPrimaryRecord().put(entry.getKey(), entry.getValue());
            }
        }

        // Apply statusLabel — edge execution sets the record's current status
        if (edge.getStatusLabel() != null && !edge.getStatusLabel().isBlank()) {
            instance.getPrimaryRecord().put("status", edge.getStatusLabel());
        }

        if (edge.isTerminal()) {
            if ("cancel".equalsIgnoreCase(edge.getName())) {
                instance.setStatus(InstanceStatus.CANCELLED);
            } else {
                instance.setStatus(InstanceStatus.COMPLETED);
            }
            instance.setCompletedAt(Instant.now());
        } else {
            if (edge.getTargetStepId() == null) {
                throw new IllegalStateException("Non-terminal edge must have targetStepId");
            }
            instance.setCurrentStepId(edge.getTargetStepId());
        }

        InstanceHistory history = new InstanceHistory();
        history.setStepId(currentStep.getId());
        history.setEdgeId(edge.getId());
        history.setEdgeName(edge.getName());
        history.setPerformedBy(userId);
        history.setPerformedByName(performedByName);
        history.setPerformedAt(Instant.now());
        history.setComment(comment);
        history.setRecordId(null);
        history.setFormData(new HashMap<>(payload));
        instance.getHistory().add(history);

        for (AutoAction autoAction : edge.getAutoActions()) {
            log.info("Auto-action '{}' skipped in Phase 1", autoAction.getType());
        }

        instanceRepository.save(instance);

        ExecuteEdgeResponse response = new ExecuteEdgeResponse();
        response.setInstanceId(instance.getId());
        response.setStatus(instance.getStatus().name());
        response.setCurrentStepId(instance.getCurrentStepId());
        response.setPreviousEdge(edge.getName());

        WorkflowStep nextStep = definition.findStepById(instance.getCurrentStepId());
        response.setNextStepName(nextStep != null ? nextStep.getName() : null);

        return response;
    }

    public StepViewResponse getStepView(String instanceId, String userId) {
        WorkflowInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow instance not found: " + instanceId));
        WorkflowDefinition definition = definitionRepository.findById(instance.getWorkflowDefinitionId())
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow definition not found"));
        WorkflowStep step = definition.findStepById(instance.getCurrentStepId());
        if (step == null) {
            throw new WorkflowNotFoundException("Current step not found in workflow definition");
        }

        StepViewResponse response = new StepViewResponse();
        response.setInstanceId(instanceId);
        response.setStepId(step.getId());
        response.setStepName(step.getName());
        response.setHistory(instance.getHistory());

        // Build visible fields: editable (this step's own fields) + readonly (from prior steps)
        List<com.adaptivebp.modules.formbuilder.model.DomainModelField> allVisible = new ArrayList<>(
                step.getFields() != null ? step.getFields() : List.of());
        List<String> roKeys = step.getReadonlyFieldKeys() != null ? step.getReadonlyFieldKeys() : List.of();
        Set<String> editableKeys = new HashSet<>();
        for (com.adaptivebp.modules.formbuilder.model.DomainModelField f : allVisible) {
            editableKeys.add(f.getKey());
        }
        for (String roKey : roKeys) {
            if (!editableKeys.contains(roKey)) {
                com.adaptivebp.modules.formbuilder.model.DomainModelField field =
                        findFieldInDefinition(definition, roKey);
                if (field != null) allVisible.add(field);
            }
        }
        response.setModelFields(allVisible);

        // Pre-fill all fields from the accumulated primary record
        response.setCurrentData(new HashMap<>(instance.getPrimaryRecord()));

        // Mark readonly fields
        response.setReadOnlyFields(new ArrayList<>(roKeys));

        // Evaluate available edges against the primary record
        Set<AppPermission> userPermissions = loadUserPermissions(instance.getAppId(), userId);
        Set<String> roles = loadUserRoles(instance.getDomainId(), userId);
        boolean isSubmitter = userId != null && userId.equals(instance.getStartedBy());

        // Return empty edge list only if user has no app permission, no workflow roles,
        // and is not the submitter (who may have onlySubmitter edges)
        if (!canExecuteWorkflow(userPermissions) && roles.isEmpty() && !isSubmitter) {
            response.setAvailableEdges(List.of());
            return response;
        }

        List<WorkflowEdge> permittedEdges = collectPermittedEdges(definition, step, roles, userId, instance.getStartedBy());
        List<StepViewResponse.EdgeView> edgeViews = new ArrayList<>();
        Map<String, Object> primaryForConditions = new HashMap<>(instance.getPrimaryRecord());

        for (WorkflowEdge edge : permittedEdges) {
            StepViewResponse.EdgeView edgeView = new StepViewResponse.EdgeView();
            edgeView.setId(edge.getId());
            edgeView.setName(edge.getName());
            if (!conditionsPass(edge.getConditions(), definition, instance, step, primaryForConditions)) {
                edgeView.setDisabled(true);
                edgeView.setDisabledReason("Condition not met");
            }
            edgeViews.add(edgeView);
        }
        response.setAvailableEdges(edgeViews);

        return response;
    }

    /** Searches all steps in the definition for a field with the given key. */
    private com.adaptivebp.modules.formbuilder.model.DomainModelField findFieldInDefinition(
            WorkflowDefinition definition, String fieldKey) {
        for (WorkflowStep s : definition.getSteps()) {
            if (s.getFields() == null) continue;
            for (com.adaptivebp.modules.formbuilder.model.DomainModelField f : s.getFields()) {
                if (fieldKey.equals(f.getKey())) return f;
            }
        }
        return null;
    }


    public List<WorkflowInstance> listInstances(String domainId, String appId) {
        return instanceRepository.findByDomainIdAndAppId(domainId, appId);
    }

    public List<WorkflowInstance> listStartedBy(String domainId, String appId, String userId) {
        return instanceRepository.findByDomainIdAndAppIdAndStartedBy(domainId, appId, userId);
    }

    public WorkflowInstance getInstance(String instanceId) {
        return instanceRepository.findById(instanceId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow instance not found: " + instanceId));
    }

    public HistoryResponse getHistory(String instanceId) {
        WorkflowInstance instance = getInstance(instanceId);
        HistoryResponse response = new HistoryResponse();
        response.setHistory(instance.getHistory());
        return response;
    }

    public TaskListResponse getMyTasks(String userId, String domainId, String appId) {
        Set<String> userRoles = loadUserRoles(domainId, userId);

        // Only bail out early if the user has absolutely no pathway to any edge:
        // no app execute permission AND no workflow roles. Even then, they could
        // be the startedBy of an instance with onlySubmitter edges, so we proceed
        // to the instance loop and let collectPermittedEdges do the filtering.
        // The loop is the authoritative gate — no duplicate coarse-guard needed.

        List<WorkflowInstance> instances = instanceRepository
                .findByDomainIdAndAppIdAndStatus(domainId, appId, InstanceStatus.ACTIVE);

        Map<String, WorkflowDefinition> definitionCache = new HashMap<>();
        List<TaskResponse> tasks = new ArrayList<>();

        for (WorkflowInstance instance : instances) {
            WorkflowDefinition definition = definitionCache.computeIfAbsent(
                    instance.getWorkflowDefinitionId(),
                    this::loadWorkflowDefinitionOrNull);
            if (definition == null) {
                continue;
            }

            WorkflowStep currentStep = definition.findStepById(instance.getCurrentStepId());
            if (currentStep == null) {
                continue;
            }

            List<WorkflowEdge> permitted = collectPermittedEdges(
                    definition,
                    currentStep,
                    userRoles,
                    userId,
                    instance.getStartedBy());

            if (permitted.isEmpty()) {
                continue;
            }

            TaskResponse task = new TaskResponse();
            task.setInstanceId(instance.getId());
            task.setWorkflowDefinitionId(instance.getWorkflowDefinitionId());
            task.setWorkflowName(definition.getName());
            task.setCurrentStepId(instance.getCurrentStepId());
            task.setCurrentStepName(currentStep.getName());
            task.setStartedAt(instance.getStartedAt());
            task.setWaitingSince(lastPerformedAt(instance));

            Map<String, Object> startedBy = new HashMap<>();
            startedBy.put("id", instance.getStartedBy());
            startedBy.put("name", instance.getStartedBy());
            task.setStartedBy(startedBy);

            List<Map<String, Object>> edges = new ArrayList<>();
            for (WorkflowEdge edge : permitted) {
                if (conditionsPass(edge.getConditions(), definition, instance, currentStep, instance.getPrimaryRecord())) {
                    Map<String, Object> edgeMap = new HashMap<>();
                    edgeMap.put("id", edge.getId());
                    edgeMap.put("name", edge.getName());
                    edges.add(edgeMap);
                }
            }

            if (edges.isEmpty()) {
                continue;
            }

            task.setAvailableEdges(edges);
            task.setSummary(extractSummaryFromRecord(instance));
            tasks.add(task);
        }

        tasks.sort((a, b) -> {
            if (a.getStartedAt() == null && b.getStartedAt() == null) {
                return 0;
            }
            if (a.getStartedAt() == null) {
                return 1;
            }
            if (b.getStartedAt() == null) {
                return -1;
            }
            return a.getStartedAt().compareTo(b.getStartedAt());
        });

        TaskListResponse response = new TaskListResponse();
        response.setTasks(tasks);
        return response;
    }

    private void validateEdgeRequiredFields(WorkflowEdge edge,
            Map<String, Object> mergedCurrentData,
            Map<String, String> fieldErrors) {
        for (String requiredField : edge.getRequiredFields()) {
            Object value = mergedCurrentData.get(requiredField);
            if (value == null || (value instanceof String str && str.trim().isEmpty())) {
                fieldErrors.put(requiredField, "Field is required for edge '" + edge.getName() + "'");
            }
        }
    }

    private Map<String, Object> extractSummaryFromRecord(WorkflowInstance instance) {
        Map<String, Object> record = instance.getPrimaryRecord();
        if (record == null || record.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, Object> summary = new HashMap<>();
        int count = 0;
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            if (!entry.getKey().startsWith("_")) {
                summary.put(entry.getKey(), entry.getValue());
                if (++count >= 6) break;
            }
        }
        return summary;
    }

    private Instant lastPerformedAt(WorkflowInstance instance) {
        if (instance.getHistory() == null || instance.getHistory().isEmpty()) {
            return instance.getStartedAt();
        }
        return instance.getHistory().get(instance.getHistory().size() - 1).getPerformedAt();
    }

    private WorkflowDefinition loadWorkflowDefinitionOrNull(String definitionId) {
        return definitionRepository.findById(definitionId).orElse(null);
    }

    private WorkflowEdge resolveEdge(WorkflowDefinition definition, WorkflowStep currentStep, String edgeId) {
        WorkflowEdge edge = currentStep.getEdges().stream()
                .filter(e -> edgeId.equals(e.getId()))
                .findFirst()
                .orElse(null);

        if (edge == null) {
            edge = definition.getGlobalEdges().stream()
                    .filter(e -> edgeId.equals(e.getId()))
                    .findFirst()
                    .orElse(null);
        }

        if (edge == null) {
            throw new EdgeNotFoundException("Edge not found: " + edgeId);
        }

        return edge;
    }

    private boolean hasEdgePermission(
            WorkflowEdge edge,
            Set<String> userRoles,
            String userId,
            String submitterId) {
        boolean hasRoleRules = edge.getAllowedRoles() != null && !edge.getAllowedRoles().isEmpty();
        boolean hasUserRules = edge.getAllowedUserIds() != null && !edge.getAllowedUserIds().isEmpty();
        boolean requiresSubmitter = edge.isOnlySubmitter();

        if (!hasRoleRules && !hasUserRules && !requiresSubmitter) {
            return true;
        }

        boolean roleMatch = intersectsIgnoreCase(userRoles, edge.getAllowedRoles());
        boolean userMatch = edge.getAllowedUserIds() != null && edge.getAllowedUserIds().contains(userId);
        boolean submitterMatch = requiresSubmitter && Objects.equals(userId, submitterId);
        return roleMatch || userMatch || submitterMatch;
    }

    private Set<String> loadUserRoles(String domainId, String userId) {
        List<DomainGroupMember> memberships = domainGroupMemberRepository.findByDomainIdAndUserId(domainId, userId);
        if (memberships.isEmpty()) {
            return Set.of();
        }

        Set<String> groupIds = new HashSet<>();
        for (DomainGroupMember member : memberships) {
            groupIds.add(member.getDomainGroupId());
        }

        List<DomainGroup> groups = domainGroupRepository.findAllById(groupIds);
        Set<String> roleNames = new HashSet<>();
        for (DomainGroup group : groups) {
            if (group.getGroupType() == DomainGroupType.WORKFLOW_ROLE && group.getName() != null) {
                roleNames.add(group.getName().toLowerCase(Locale.ROOT));
            }
        }
        return roleNames;
    }

    private Set<AppPermission> loadUserPermissions(String appId, String userId) {
        List<AppGroup> groups = loadUserAppGroups(appId, userId);
        Set<AppPermission> permissions = new HashSet<>();
        for (AppGroup group : groups) {
            if (group.getPermissions() != null) {
                permissions.addAll(group.getPermissions());
            }
        }
        return permissions;
    }

    private List<AppGroup> loadUserAppGroups(String appId, String userId) {
        List<AppGroupMember> memberships = appGroupMemberRepository.findByAppIdAndUserId(appId, userId);
        if (memberships.isEmpty()) {
            return List.of();
        }

        Set<String> groupIds = new HashSet<>();
        for (AppGroupMember member : memberships) {
            groupIds.add(member.getGroupId());
        }
        return appGroupRepository.findAllById(groupIds);
    }

    private boolean canExecuteWorkflow(Set<AppPermission> permissions) {
        return permissions.contains(AppPermission.APP_EXECUTE_WORKFLOW)
                || permissions.contains(AppPermission.APP_EXECUTE);
    }

    private boolean intersectsIgnoreCase(Set<String> roleNames, Collection<String> allowedRoles) {
        if (allowedRoles == null || allowedRoles.isEmpty()) {
            return false;
        }
        for (String allowed : allowedRoles) {
            if (allowed != null && roleNames.contains(allowed.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private List<WorkflowEdge> collectPermittedEdges(WorkflowDefinition definition,
            WorkflowStep step,
            Set<String> roleNames,
            String userId,
            String startedBy) {
        List<WorkflowEdge> allEdges = new ArrayList<>();
        allEdges.addAll(step.getEdges());
        allEdges.addAll(definition.getGlobalEdges());

        List<WorkflowEdge> filtered = new ArrayList<>();
        for (WorkflowEdge edge : allEdges) {
            if (hasEdgePermission(edge, roleNames, userId, startedBy)) {
                filtered.add(edge);
            }
        }
        return filtered;
    }



    private boolean conditionsPass(
            List<EdgeCondition> conditions,
            WorkflowDefinition definition,
            WorkflowInstance instance,
            WorkflowStep currentStep,
            Map<String, Object> currentData) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        for (EdgeCondition condition : conditions) {
            Object actualValue = resolveConditionValue(condition.getField(), definition, instance, currentStep, currentData);
            if (!evaluateCondition(actualValue, condition.getOperator(), condition.getValue())) {
                return false;
            }
        }
        return true;
    }

    private Object resolveConditionValue(
            String field,
            WorkflowDefinition definition,
            WorkflowInstance instance,
            WorkflowStep currentStep,
            Map<String, Object> currentData) {
        if (field == null) return null;
        // Primary lookup: currentData (which is primaryRecord merged with payload)
        if (currentData != null && currentData.containsKey(field)) {
            return currentData.get(field);
        }
        // Fallback: look in primaryRecord directly
        return instance.getPrimaryRecord().get(field);
    }

    private boolean evaluateCondition(Object actual, ConditionOperator operator, Object expected) {
        return switch (operator) {
            case EQUALS -> Objects.equals(actual, expected);
            case NOT_EQUALS -> !Objects.equals(actual, expected);
            case IS_EMPTY -> isEmpty(actual);
            case IS_NOT_EMPTY -> !isEmpty(actual);
            case CONTAINS -> contains(actual, expected);
            case GREATER_THAN -> compareAsNumber(actual, expected) > 0;
            case LESS_THAN -> compareAsNumber(actual, expected) < 0;
            case GREATER_EQUAL -> compareAsNumber(actual, expected) >= 0;
            case LESS_EQUAL -> compareAsNumber(actual, expected) <= 0;
        };
    }

    private int compareAsNumber(Object left, Object right) {
        Double l = toDouble(left);
        Double r = toDouble(right);
        if (l == null || r == null) {
            return Integer.MIN_VALUE;
        }
        return Double.compare(l, r);
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Map<String, String> validateStepData(
            WorkflowStep step,
            Map<String, Object> data,
            boolean requireRequiredFields) {
        Map<String, String> errors = new HashMap<>();
        List<DomainModelField> fields = step.getFields() != null ? step.getFields() : List.of();

        for (DomainModelField field : fields) {
            Object value = data.get(field.getKey());
            if (requireRequiredFields && field.isRequired() && isEmpty(value)) {
                errors.put(field.getKey(), "Field is required");
                continue;
            }
            if (value == null) {
                continue;
            }
            if (!matchesType(field.getType(), value)) {
                errors.put(field.getKey(), "Invalid type for " + field.getType());
            }
        }

        return errors;
    }

    private boolean matchesType(DomainFieldType type, Object value) {
        return switch (type) {
            case STRING -> value instanceof String;
            case NUMBER -> value instanceof Number;
            case BOOLEAN -> value instanceof Boolean;
            case DATE, DATETIME -> value instanceof Date
                    || value instanceof Instant
                    || value instanceof LocalDate
                    || value instanceof LocalDateTime
                    || value instanceof String;
            case REFERENCE, EMPLOYEE_REFERENCE -> value instanceof String;
            case OBJECT -> value instanceof Map;
            case ARRAY -> value instanceof List;
        };
    }

    private boolean contains(Object container, Object item) {
        if (container == null) {
            return false;
        }
        if (container instanceof String string) {
            return item != null && string.contains(String.valueOf(item));
        }
        if (container instanceof Collection<?> collection) {
            return collection.contains(item);
        }
        return false;
    }

    private Map<String, Object> getStepRecordMap(WorkflowInstance instance, String stepId) {
        if (instance == null || stepId == null) {
            return null;
        }
        Object value = instance.getStepRecords().get(stepId);
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> typed = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    typed.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            return typed;
        }
        return null;
    }

    private void putStepRecord(WorkflowInstance instance, String stepId, Map<String, Object> data) {
        if (instance == null || stepId == null) {
            return;
        }
        instance.getStepRecords().put(stepId, data != null ? data : new HashMap<>());
    }

    private boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String string) {
            return string.trim().isEmpty();
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        return false;
    }
}
