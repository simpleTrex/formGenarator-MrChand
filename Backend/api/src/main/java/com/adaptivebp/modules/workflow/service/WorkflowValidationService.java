package com.adaptivebp.modules.workflow.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.adaptivebp.modules.formbuilder.model.DomainModelField;
import com.adaptivebp.modules.workflow.dto.response.ValidationResult;
import com.adaptivebp.modules.workflow.model.AutoFetchRule;
import com.adaptivebp.modules.workflow.model.WorkflowDefinition;
import com.adaptivebp.modules.workflow.model.WorkflowEdge;
import com.adaptivebp.modules.workflow.model.WorkflowStep;

@Service
public class WorkflowValidationService {

    public ValidationResult validate(WorkflowDefinition definition) {
        List<String> errors = new ArrayList<>();
        List<WorkflowStep> steps = definition.getSteps() != null ? definition.getSteps() : List.of();
        List<WorkflowEdge> globalEdges = definition.getGlobalEdges() != null ? definition.getGlobalEdges() : List.of();

        if (steps.isEmpty()) {
            errors.add("Workflow must contain at least one step");
            return ValidationResult.fail(errors);
        }

        long startCount = steps.stream().filter(WorkflowStep::isStart).count();
        if (startCount != 1) {
            errors.add("Workflow must contain exactly one start step");
        }

        boolean hasEndStep = steps.stream().anyMatch(WorkflowStep::isEnd);
        boolean hasTerminalEdge = steps.stream().flatMap(s -> s.getEdges().stream()).anyMatch(WorkflowEdge::isTerminal)
                || globalEdges.stream().anyMatch(WorkflowEdge::isTerminal);
        if (!hasEndStep && !hasTerminalEdge) {
            errors.add("Workflow must have at least one end step or one terminal edge");
        }

        Set<String> stepIds = new HashSet<>();
        Map<String, WorkflowStep> stepById = new HashMap<>();
        for (WorkflowStep step : steps) {
            if (isBlank(step.getId())) {
                errors.add("Step id cannot be blank");
                continue;
            }
            if (!stepIds.add(step.getId())) {
                errors.add("Duplicate step id: " + step.getId());
            }
            stepById.put(step.getId(), step);
        }

        for (WorkflowStep step : steps) {
            Set<String> edgeIds = new HashSet<>();
            for (WorkflowEdge edge : step.getEdges()) {
                if (isBlank(edge.getId())) {
                    errors.add("Edge id cannot be blank in step " + step.getId());
                } else if (!edgeIds.add(edge.getId())) {
                    errors.add("Duplicate edge id in step " + step.getId() + ": " + edge.getId());
                }

                if (!isBlank(edge.getTargetStepId()) && !stepById.containsKey(edge.getTargetStepId())) {
                    errors.add("Edge '" + edge.getId() + "' in step '" + step.getId()
                            + "' references unknown targetStepId '" + edge.getTargetStepId() + "'");
                }

                if (isNoPermission(edge)) {
                    errors.add("Edge '" + edge.getId() + "' in step '" + step.getId()
                            + "' must define allowedRoles, allowedUserIds, or onlySubmitter=true");
                }
            }
        }

        for (WorkflowStep step : steps) {
            List<DomainModelField> fields = step.getFields() != null ? step.getFields() : List.of();
            Set<String> fieldKeys = new HashSet<>();
            for (DomainModelField field : fields) {
                if (isBlank(field.getKey())) {
                    errors.add("Step '" + step.getId() + "' has a field with a blank key");
                    continue;
                }
                if (!fieldKeys.add(field.getKey())) {
                    errors.add("Step '" + step.getId() + "' has duplicate field key '" + field.getKey() + "'");
                }
            }

            for (WorkflowEdge edge : step.getEdges()) {
                for (String requiredField : edge.getRequiredFields()) {
                    if (!fieldKeys.contains(requiredField)) {
                        errors.add("Edge '" + edge.getId() + "' required field '" + requiredField
                                + "' is not present in step fields for '" + step.getId() + "'");
                    }
                }
            }

            if (step.getDataConfig() != null && step.getDataConfig().getReuseFromStepId() != null
                    && !step.getDataConfig().getReuseFromStepId().isBlank()) {
                WorkflowStep reused = stepById.get(step.getDataConfig().getReuseFromStepId());
                if (reused == null) {
                    errors.add("Step '" + step.getId() + "' reuseFromStepId references unknown step '"
                            + step.getDataConfig().getReuseFromStepId() + "'");
                } else if (reused.getOrder() >= step.getOrder()) {
                    errors.add("Step '" + step.getId() + "' reuseFromStepId must point to a previous step");
                } else if (!fieldKeys.equals(fieldKeysFor(reused))) {
                    errors.add("Step '" + step.getId() + "' reuseFromStepId must reference a step with the same fields");
                }
            }

            if (step.getDataConfig() != null && step.getDataConfig().getAutoFetchRules() != null) {
                for (AutoFetchRule rule : step.getDataConfig().getAutoFetchRules()) {
                    WorkflowStep sourceStep = stepById.get(rule.getSourceStepId());
                    if (sourceStep == null) {
                        errors.add("Auto fetch rule in step '" + step.getId() + "' references unknown source step '"
                            + rule.getSourceStepId() + "'");
                        continue;
                    }
                    if (sourceStep.getOrder() >= step.getOrder()) {
                        errors.add("Auto fetch rule in step '" + step.getId() + "' must reference a previous step");
                    }
                    boolean sourceFieldOk = fieldKeysFor(sourceStep).contains(rule.getSourceField());
                    if (!sourceFieldOk) {
                        errors.add("Auto fetch rule in step '" + step.getId() + "' has unknown source field '"
                                + rule.getSourceField() + "'");
                    }
                    if (rule.getTargetField() != null && !rule.getTargetField().isBlank()
                            && !fieldKeys.contains(rule.getTargetField())) {
                        errors.add("Auto fetch rule in step '" + step.getId() + "' has unknown target field '"
                                + rule.getTargetField() + "'");
                    }
                }
            }
        }

        WorkflowStep start = steps.stream().filter(WorkflowStep::isStart).findFirst().orElse(null);
        if (start != null) {
            Set<String> reachable = findReachable(start, steps);
            for (WorkflowStep step : steps) {
                if (!step.isStart() && !reachable.contains(step.getId())) {
                    errors.add("Step '" + step.getId() + "' is orphaned and not reachable from start");
                }
            }
        }

        for (WorkflowEdge globalEdge : globalEdges) {
            if (!globalEdge.isTerminal() && (isBlank(globalEdge.getTargetStepId()) || !stepById.containsKey(globalEdge.getTargetStepId()))) {
                errors.add("Global edge '" + globalEdge.getId() + "' must be terminal or have a valid targetStepId");
            }
            if (isNoPermission(globalEdge)) {
                errors.add("Global edge '" + globalEdge.getId() + "' must define allowedRoles, allowedUserIds, or onlySubmitter=true");
            }
        }

        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(errors);
    }

    private Set<String> findReachable(WorkflowStep start, List<WorkflowStep> steps) {
        Map<String, WorkflowStep> byId = new HashMap<>();
        for (WorkflowStep step : steps) {
            byId.put(step.getId(), step);
        }

        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        queue.add(start.getId());
        visited.add(start.getId());

        while (!queue.isEmpty()) {
            String current = queue.poll();
            WorkflowStep currentStep = byId.get(current);
            if (currentStep == null) {
                continue;
            }

            for (WorkflowEdge edge : currentStep.getEdges()) {
                if (!isBlank(edge.getTargetStepId()) && visited.add(edge.getTargetStepId())) {
                    queue.add(edge.getTargetStepId());
                }
            }
        }

        return visited;
    }

    private Set<String> fieldKeysFor(WorkflowStep step) {
        List<DomainModelField> fields = step.getFields() != null ? step.getFields() : List.of();
        Set<String> fieldKeys = new HashSet<>();
        for (DomainModelField field : fields) {
            if (!isBlank(field.getKey())) {
                fieldKeys.add(field.getKey());
            }
        }
        return fieldKeys;
    }

    private boolean isNoPermission(WorkflowEdge edge) {
        return (edge.getAllowedRoles() == null || edge.getAllowedRoles().isEmpty())
                && (edge.getAllowedUserIds() == null || edge.getAllowedUserIds().isEmpty())
                && !edge.isOnlySubmitter();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
