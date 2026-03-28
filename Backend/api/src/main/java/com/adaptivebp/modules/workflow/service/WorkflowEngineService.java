package com.adaptivebp.modules.workflow.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import com.adaptivebp.modules.appmanagement.repository.AppGroupMemberRepository;
import com.adaptivebp.modules.appmanagement.repository.AppGroupRepository;
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
import com.adaptivebp.modules.workflow.model.AutoFetchRule;
import com.adaptivebp.modules.workflow.model.EdgeCondition;
import com.adaptivebp.modules.workflow.model.InstanceHistory;
import com.adaptivebp.modules.workflow.model.StepDataConfig;
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
        if (!payload.isEmpty()) {
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

        Set<String> userRoles = loadUserRoles(instance.getAppId(), userId);
        if (!hasEdgePermission(edge, userRoles, userId, instance.getStartedBy())) {
            throw new InsufficientEdgePermissionException(
                    "You do not have permission to execute '" + edge.getName() + "'");
        }

        Map<String, Object> currentRecordData = getStepRecordData(definition, currentStep, instance);
        Map<String, Object> payload = formData != null ? new HashMap<>(formData) : new HashMap<>();
        Map<String, Object> mergedCurrentData = new HashMap<>();
        if (currentRecordData != null) {
            mergedCurrentData.putAll(currentRecordData);
        }
        mergedCurrentData.putAll(payload);

        if (!conditionsPass(edge.getConditions(), definition, instance, currentStep, mergedCurrentData)) {
            throw new ConditionNotMetException("Conditions for edge '" + edge.getName() + "' are not met");
        }

        Map<String, Object> existingRecordData = getStepRecordMap(instance, currentStep.getId());
        StepDataConfig dataConfig = currentStep.getDataConfig();
        if (dataConfig != null && dataConfig.getReuseFromStepId() != null && !dataConfig.getReuseFromStepId().isBlank()) {
            Map<String, Object> reusedRecordData = getStepRecordMap(instance, dataConfig.getReuseFromStepId());
            if (reusedRecordData == null) {
                throw new IllegalStateException("Reused step record not found for step " + dataConfig.getReuseFromStepId());
            }
            existingRecordData = reusedRecordData;
        }
        Map<String, Object> recordData;
        if (dataConfig != null && dataConfig.getReuseFromStepId() != null && !dataConfig.getReuseFromStepId().isBlank()) {
            recordData = existingRecordData != null ? new HashMap<>(existingRecordData) : new HashMap<>();
            putStepRecord(instance, currentStep.getId(), recordData);
        } else {
            boolean requireStepRequiredFields = existingRecordData == null || existingRecordData.isEmpty();
            Map<String, String> fieldErrors = validateStepData(currentStep, payload, requireStepRequiredFields);

            validateEdgeRequiredFields(edge, mergedCurrentData, fieldErrors);
            if (!fieldErrors.isEmpty()) {
                throw new InvalidFormDataException("Invalid form data", fieldErrors);
            }

            if (existingRecordData != null) {
                recordData = new HashMap<>(existingRecordData);
                recordData.putAll(payload);
            } else {
                recordData = new HashMap<>(payload);
            }
            putStepRecord(instance, currentStep.getId(), recordData);
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

        response.setModelFields(step.getFields());

        Map<String, Object> currentData = getStepRecordData(definition, step, instance);
        response.setCurrentData(currentData != null ? currentData : new HashMap<>());

        StepDataConfig dataConfig = step.getDataConfig();
        if (dataConfig != null) {
            response.setReadOnlyFields(dataConfig.getReadOnlyFields());

            if (dataConfig.getReuseFromStepId() != null && !dataConfig.getReuseFromStepId().isBlank()) {
                Map<String, Object> reused = getReusedStepData(definition, instance, dataConfig.getReuseFromStepId());
                response.setCurrentData(reused);
                response.setReadOnlyFields(allFieldKeys(step.getFields()));
            } else if (dataConfig.isReferencePreviousStep()) {
                Map<String, Object> referenced = loadReferencedData(definition, instance, step, dataConfig);
                response.setReferencedData(referenced);
            }

            if (dataConfig.getAutoFetchRules() != null && !dataConfig.getAutoFetchRules().isEmpty()) {
                Map<String, Object> mappedData = loadMappedData(definition, instance, dataConfig);
                response.setMappedData(mappedData);
                mergeIntoCurrentData(response.getCurrentData(), mappedData);
            }
        }

        Set<String> roles = loadUserRoles(instance.getAppId(), userId);
        List<WorkflowEdge> permittedEdges = collectPermittedEdges(definition, step, roles, userId, instance.getStartedBy());
        List<StepViewResponse.EdgeView> edgeViews = new ArrayList<>();

        Map<String, Object> mergedCurrent = new HashMap<>();
        if (currentData != null) {
            mergedCurrent.putAll(currentData);
        }

        for (WorkflowEdge edge : permittedEdges) {
            StepViewResponse.EdgeView edgeView = new StepViewResponse.EdgeView();
            edgeView.setId(edge.getId());
            edgeView.setName(edge.getName());

            if (!conditionsPass(edge.getConditions(), definition, instance, step, mergedCurrent)) {
                edgeView.setDisabled(true);
                edgeView.setDisabledReason("Condition not met");
            }

            edgeViews.add(edgeView);
        }
        response.setAvailableEdges(edgeViews);

        return response;
    }

    private Map<String, Object> getReusedStepData(
            WorkflowDefinition definition,
            WorkflowInstance instance,
            String reuseFromStepId) {
        WorkflowStep reusedStep = definition.findStepById(reuseFromStepId);
        if (reusedStep == null) {
            return new HashMap<>();
        }
        Map<String, Object> recordData = getStepRecordMap(instance, reuseFromStepId);
        return recordData != null ? new HashMap<>(recordData) : new HashMap<>();
    }

    private List<String> allFieldKeys(List<DomainModelField> fields) {
        List<String> keys = new ArrayList<>();
        if (fields == null) {
            return keys;
        }
        for (DomainModelField field : fields) {
            keys.add(field.getKey());
        }
        return keys;
    }

    private void mergeIntoCurrentData(Map<String, Object> currentData, Map<String, Object> mappedData) {
        if (currentData == null || mappedData == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : mappedData.entrySet()) {
            Object existing = currentData.get(entry.getKey());
            if (existing == null || (existing instanceof String str && str.isBlank())) {
                currentData.put(entry.getKey(), entry.getValue());
            }
        }
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
        Set<String> userRoles = loadUserRoles(appId, userId);

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
            task.setWorkflowName(definition.getName());
            task.setCurrentStepName(currentStep.getName());
            task.setStartedAt(instance.getStartedAt());
            task.setWaitingSince(lastPerformedAt(instance));

            Map<String, Object> startedBy = new HashMap<>();
            startedBy.put("id", instance.getStartedBy());
            startedBy.put("name", instance.getStartedBy());
            task.setStartedBy(startedBy);

            List<Map<String, Object>> edges = new ArrayList<>();
            for (WorkflowEdge edge : permitted) {
                if (conditionsPass(edge.getConditions(), definition, instance, currentStep, getStepRecordData(definition, currentStep, instance))) {
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
            task.setSummary(extractSummary(definition, instance, currentStep));
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

    private Map<String, Object> extractSummary(
            WorkflowDefinition definition,
            WorkflowInstance instance,
            WorkflowStep step) {
        Map<String, Object> record = getStepRecordData(definition, step, instance);
        if (record == null || record.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> summary = new HashMap<>();
        int count = 0;
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("_")) {
                continue;
            }
            summary.put(key, entry.getValue());
            count++;
            if (count >= 6) {
                break;
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
        boolean roleMatch = intersectsIgnoreCase(userRoles, edge.getAllowedRoles());
        boolean userMatch = edge.getAllowedUserIds() != null && edge.getAllowedUserIds().contains(userId);
        boolean submitterMatch = edge.isOnlySubmitter() && Objects.equals(userId, submitterId);
        return roleMatch || userMatch || submitterMatch;
    }

    private Set<String> loadUserRoles(String appId, String userId) {
        List<AppGroupMember> memberships = appGroupMemberRepository.findByAppIdAndUserId(appId, userId);
        if (memberships.isEmpty()) {
            return Set.of();
        }

        Set<String> groupIds = new HashSet<>();
        for (AppGroupMember member : memberships) {
            groupIds.add(member.getGroupId());
        }

        List<AppGroup> groups = appGroupRepository.findAllById(groupIds);
        Set<String> roleNames = new HashSet<>();
        for (AppGroup group : groups) {
            if (group.getName() != null) {
                roleNames.add(group.getName().toLowerCase(Locale.ROOT));
            }
        }
        return roleNames;
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

    private Map<String, Object> getStepRecordData(
            WorkflowDefinition definition,
            WorkflowStep step,
            WorkflowInstance instance) {
        Map<String, Object> recordData = getStepRecordMap(instance, step.getId());
        StepDataConfig config = step.getDataConfig();
        if (recordData == null && config != null && config.getReuseFromStepId() != null) {
            recordData = getStepRecordMap(instance, config.getReuseFromStepId());
        }
        if (recordData == null) {
            return null;
        }
        return new HashMap<>(recordData);
    }

    private Map<String, Object> loadReferencedData(
            WorkflowDefinition definition,
            WorkflowInstance instance,
            WorkflowStep currentStep,
            StepDataConfig dataConfig) {
        if (dataConfig.getReuseFromStepId() != null && !dataConfig.getReuseFromStepId().isBlank()) {
            return Collections.emptyMap();
        }
        String previousStepId = findPreviousStepId(instance, currentStep.getId());
        if (previousStepId == null) {
            return Collections.emptyMap();
        }

        WorkflowStep previousStep = definition.findStepById(previousStepId);
        if (previousStep == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> previousData = getStepRecordMap(instance, previousStepId);
        if (previousData == null) {
            return Collections.emptyMap();
        }

        if (dataConfig.getPreviousStepFields() == null || dataConfig.getPreviousStepFields().isEmpty()) {
            return previousData;
        }

        Map<String, Object> filtered = new HashMap<>();
        for (String key : dataConfig.getPreviousStepFields()) {
            if (previousData.containsKey(key)) {
                filtered.put(key, previousData.get(key));
            }
        }
        return filtered;
    }

    private Map<String, Object> loadMappedData(
            WorkflowDefinition definition,
            WorkflowInstance instance,
            StepDataConfig dataConfig) {
        Map<String, Object> mapped = new HashMap<>();

        for (AutoFetchRule rule : dataConfig.getAutoFetchRules()) {
            WorkflowStep sourceStep = definition.findStepById(rule.getSourceStepId());
            if (sourceStep == null) {
                continue;
            }

            Map<String, Object> sourceData = getStepRecordMap(instance, rule.getSourceStepId());
            if (sourceData == null) {
                continue;
            }
            if (sourceData == null || !sourceData.containsKey(rule.getSourceField())) {
                continue;
            }

            Object value = sourceData.get(rule.getSourceField());
            if (rule.getTargetField() != null && !rule.getTargetField().isBlank()) {
                mapped.put(rule.getTargetField(), value);
            }
        }

        return mapped;
    }

    private String findPreviousStepId(WorkflowInstance instance, String currentStepId) {
        if (instance.getHistory() == null) {
            return null;
        }

        for (int i = instance.getHistory().size() - 1; i >= 0; i--) {
            InstanceHistory history = instance.getHistory().get(i);
            if (history.getStepId() != null && !history.getStepId().equals(currentStepId)) {
                return history.getStepId();
            }
        }
        return null;
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
        if (field == null) {
            return null;
        }

        String[] parts = field.split("\\.", 2);
        if (parts.length == 2) {
            WorkflowStep step = definition.findStepById(parts[0]);
            Map<String, Object> data = getStepRecordMap(instance, parts[0]);
            if (step != null && data != null) {
                return data.get(parts[1]);
            }
        }

        if (currentData != null) {
            return currentData.get(field);
        }
        return null;
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
