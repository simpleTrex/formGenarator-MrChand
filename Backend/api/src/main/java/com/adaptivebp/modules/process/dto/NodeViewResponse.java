package com.adaptivebp.modules.process.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.adaptivebp.modules.process.model.enums.NodeType;

public class NodeViewResponse {

    private String nodeId;
    private String nodeName;
    private NodeType nodeType;
    /** The full node config (form elements, data view config, approval config, etc.) */
    private Map<String, Object> config = new HashMap<>();
    /** Pre-filled values from instance.data or instance.draftData */
    private Map<String, Object> prefilledData = new HashMap<>();
    /** For DATA_VIEW nodes — queried records */
    private List<Map<String, Object>> records;
    /** Available actions for this node (e.g. approve/reject for APPROVAL, next for FORM_PAGE) */
    private List<String> availableActions;
    /** Completion message for END nodes */
    private String completionMessage;

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public NodeType getNodeType() { return nodeType; }
    public void setNodeType(NodeType nodeType) { this.nodeType = nodeType; }

    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }

    public Map<String, Object> getPrefilledData() { return prefilledData; }
    public void setPrefilledData(Map<String, Object> prefilledData) { this.prefilledData = prefilledData; }

    public List<Map<String, Object>> getRecords() { return records; }
    public void setRecords(List<Map<String, Object>> records) { this.records = records; }

    public List<String> getAvailableActions() { return availableActions; }
    public void setAvailableActions(List<String> availableActions) { this.availableActions = availableActions; }

    public String getCompletionMessage() { return completionMessage; }
    public void setCompletionMessage(String completionMessage) { this.completionMessage = completionMessage; }
}
