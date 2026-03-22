package com.adaptivebp.modules.process.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.adaptivebp.modules.process.dto.ValidationResult;
import com.adaptivebp.modules.process.model.ProcessDefinition;
import com.adaptivebp.modules.process.model.ProcessEdge;
import com.adaptivebp.modules.process.model.ProcessNode;
import com.adaptivebp.modules.process.model.enums.NodeType;

@Service
public class ProcessValidationService {

    public ValidationResult validate(ProcessDefinition def) {
        List<String> errors = new ArrayList<>();
        List<ProcessNode> nodes = def.getNodes();
        List<ProcessEdge> edges = def.getEdges();

        if (nodes == null || nodes.isEmpty()) {
            errors.add("Process must have at least one node");
            return ValidationResult.fail(errors);
        }

        // 1 & 12 — Duplicate node IDs, exactly one START
        Set<String> nodeIds = new HashSet<>();
        List<ProcessNode> startNodes = new ArrayList<>();
        List<ProcessNode> endNodes = new ArrayList<>();

        for (ProcessNode n : nodes) {
            if (n.getId() == null || n.getId().isBlank()) {
                errors.add("A node has a blank id");
                continue;
            }
            if (!nodeIds.add(n.getId())) {
                errors.add("Duplicate node id: " + n.getId());
            }
            if (n.getType() == NodeType.START) startNodes.add(n);
            if (n.getType() == NodeType.END) endNodes.add(n);
        }

        if (startNodes.size() != 1) {
            errors.add("Process must have exactly one START node (found " + startNodes.size() + ")");
        }
        if (endNodes.isEmpty()) {
            errors.add("Process must have at least one END node");
        }

        if (!errors.isEmpty()) return ValidationResult.fail(errors);

        // 12 — Duplicate edge IDs
        Set<String> edgeIds = new HashSet<>();
        if (edges != null) {
            for (ProcessEdge e : edges) {
                if (e.getId() == null || e.getId().isBlank()) {
                    errors.add("An edge has a blank id");
                    continue;
                }
                if (!edgeIds.add(e.getId())) {
                    errors.add("Duplicate edge id: " + e.getId());
                }
                // 6 — Edges reference valid nodes
                if (!nodeIds.contains(e.getFromNodeId())) {
                    errors.add("Edge '" + e.getId() + "' fromNodeId '" + e.getFromNodeId() + "' does not exist");
                }
                if (!nodeIds.contains(e.getToNodeId())) {
                    errors.add("Edge '" + e.getId() + "' toNodeId '" + e.getToNodeId() + "' does not exist");
                }
            }
        }

        if (!errors.isEmpty()) return ValidationResult.fail(errors);

        // Build adjacency maps
        Map<String, List<ProcessEdge>> outgoing = buildOutgoing(nodes, edges);
        Map<String, List<ProcessEdge>> incoming = buildIncoming(nodes, edges);

        ProcessNode startNode = startNodes.get(0);

        // 3 — START: exactly 1 outgoing, 0 incoming
        if (outgoing.getOrDefault(startNode.getId(), List.of()).size() != 1) {
            errors.add("START node must have exactly 1 outgoing edge");
        }
        if (!incoming.getOrDefault(startNode.getId(), List.of()).isEmpty()) {
            errors.add("START node must have 0 incoming edges");
        }

        // 4 — END nodes: 0 outgoing, 1+ incoming
        for (ProcessNode end : endNodes) {
            if (!outgoing.getOrDefault(end.getId(), List.of()).isEmpty()) {
                errors.add("END node '" + end.getId() + "' must have 0 outgoing edges");
            }
            if (incoming.getOrDefault(end.getId(), List.of()).isEmpty()) {
                errors.add("END node '" + end.getId() + "' must have at least 1 incoming edge");
            }
        }

        // 13 — APPROVAL nodes: 2+ outgoing edges
        for (ProcessNode n : nodes) {
            if (n.getType() == NodeType.APPROVAL) {
                int out = outgoing.getOrDefault(n.getId(), List.of()).size();
                if (out < 2) {
                    errors.add("APPROVAL node '" + n.getId() + "' must have at least 2 outgoing edges (approve + reject)");
                }
            }
        }

        // 7 — Every non-START node is reachable from START (BFS)
        Set<String> reachableFromStart = bfsReachable(startNode.getId(), outgoing, false);
        for (ProcessNode n : nodes) {
            if (n.getType() != NodeType.START && !reachableFromStart.contains(n.getId())) {
                errors.add("Node '" + n.getId() + "' (" + n.getName() + ") is not reachable from START");
            }
        }

        // 8 — Every non-END node has a path to at least one END (reverse BFS from each END)
        Set<String> canReachEnd = new HashSet<>();
        for (ProcessNode end : endNodes) {
            canReachEnd.addAll(bfsReachable(end.getId(), incoming, true));
        }
        for (ProcessNode n : nodes) {
            if (n.getType() != NodeType.END && !canReachEnd.contains(n.getId())) {
                errors.add("Node '" + n.getId() + "' (" + n.getName() + ") has no path to any END node");
            }
        }

        // 9 — CONDITION nodes: defaultEdgeId present and all targetEdgeIds map to real edges
        for (ProcessNode n : nodes) {
            if (n.getType() == NodeType.CONDITION) {
                validateConditionNode(n, edgeIds, outgoing, errors);
            }
        }

        // 10 — FORM_PAGE nodes: every element binding.modelId in linkedModelIds
        Set<String> linkedModels = new HashSet<>(def.getLinkedModelIds() == null ? List.of() : def.getLinkedModelIds());
        for (ProcessNode n : nodes) {
            if (n.getType() == NodeType.FORM_PAGE) {
                validateFormPageNode(n, linkedModels, errors);
            }
        }

        // 11 — DATA_ACTION nodes: modelId in linkedModelIds
        for (ProcessNode n : nodes) {
            if (n.getType() == NodeType.DATA_ACTION) {
                validateDataActionNode(n, linkedModels, errors);
            }
        }

        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(errors);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void validateConditionNode(ProcessNode n, Set<String> edgeIds,
            Map<String, List<ProcessEdge>> outgoing, List<String> errors) {
        Map<String, Object> config = n.getConfig();
        if (config == null) {
            errors.add("CONDITION node '" + n.getId() + "' has no config");
            return;
        }
        String defaultEdgeId = (String) config.get("defaultEdgeId");
        if (defaultEdgeId == null || defaultEdgeId.isBlank()) {
            errors.add("CONDITION node '" + n.getId() + "' missing defaultEdgeId");
        } else if (!edgeIds.contains(defaultEdgeId)) {
            errors.add("CONDITION node '" + n.getId() + "' defaultEdgeId '" + defaultEdgeId + "' not found");
        }
        List<Map<String, Object>> rules = (List<Map<String, Object>>) config.get("rules");
        if (rules != null) {
            for (Map<String, Object> rule : rules) {
                String targetEdgeId = (String) rule.get("targetEdgeId");
                if (targetEdgeId != null && !edgeIds.contains(targetEdgeId)) {
                    errors.add("CONDITION node '" + n.getId() + "' rule targetEdgeId '" + targetEdgeId + "' not found");
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void validateFormPageNode(ProcessNode n, Set<String> linkedModels, List<String> errors) {
        Map<String, Object> config = n.getConfig();
        if (config == null) return;
        List<Map<String, Object>> elements = (List<Map<String, Object>>) config.get("elements");
        if (elements == null) return;
        for (Map<String, Object> el : elements) {
            Map<String, Object> binding = (Map<String, Object>) el.get("binding");
            if (binding != null) {
                String modelId = (String) binding.get("modelId");
                if (modelId != null && !linkedModels.contains(modelId)) {
                    errors.add("FORM_PAGE node '" + n.getId() + "' element binding.modelId '" + modelId
                            + "' is not in linkedModelIds");
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void validateDataActionNode(ProcessNode n, Set<String> linkedModels, List<String> errors) {
        Map<String, Object> config = n.getConfig();
        if (config == null) {
            errors.add("DATA_ACTION node '" + n.getId() + "' has no config");
            return;
        }
        String modelId = (String) config.get("modelId");
        if (modelId == null || modelId.isBlank()) {
            errors.add("DATA_ACTION node '" + n.getId() + "' missing modelId");
        } else if (!linkedModels.contains(modelId)) {
            errors.add("DATA_ACTION node '" + n.getId() + "' modelId '" + modelId + "' is not in linkedModelIds");
        }
    }

    private Set<String> bfsReachable(String startId, Map<String, List<ProcessEdge>> adjacency, boolean reverse) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(startId);
        visited.add(startId);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (ProcessEdge edge : adjacency.getOrDefault(current, List.of())) {
                String next = reverse ? edge.getFromNodeId() : edge.getToNodeId();
                if (next != null && !visited.contains(next)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return visited;
    }

    private Map<String, List<ProcessEdge>> buildOutgoing(List<ProcessNode> nodes, List<ProcessEdge> edges) {
        Map<String, List<ProcessEdge>> map = new HashMap<>();
        for (ProcessNode n : nodes) map.put(n.getId(), new ArrayList<>());
        if (edges != null) {
            for (ProcessEdge e : edges) {
                map.computeIfAbsent(e.getFromNodeId(), k -> new ArrayList<>()).add(e);
            }
        }
        return map;
    }

    private Map<String, List<ProcessEdge>> buildIncoming(List<ProcessNode> nodes, List<ProcessEdge> edges) {
        Map<String, List<ProcessEdge>> map = new HashMap<>();
        for (ProcessNode n : nodes) map.put(n.getId(), new ArrayList<>());
        if (edges != null) {
            for (ProcessEdge e : edges) {
                map.computeIfAbsent(e.getToNodeId(), k -> new ArrayList<>()).add(e);
            }
        }
        return map;
    }
}
