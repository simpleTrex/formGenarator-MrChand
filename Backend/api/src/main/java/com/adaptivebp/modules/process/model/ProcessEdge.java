package com.adaptivebp.modules.process.model;

public class ProcessEdge {

    /** Unique within this process definition, e.g. "edge_1" */
    private String id;
    private String fromNodeId;
    private String toNodeId;
    private String label;
    /** For CONDITION/APPROVAL nodes — matches a rule's targetEdgeId or an action id */
    private String conditionRef;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFromNodeId() { return fromNodeId; }
    public void setFromNodeId(String fromNodeId) { this.fromNodeId = fromNodeId; }

    public String getToNodeId() { return toNodeId; }
    public void setToNodeId(String toNodeId) { this.toNodeId = toNodeId; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getConditionRef() { return conditionRef; }
    public void setConditionRef(String conditionRef) { this.conditionRef = conditionRef; }
}
