package com.adaptivebp.modules.process.model.embedded;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class HistoryEntry {

    private String nodeId;
    /** ENTERED | SUBMITTED | APPROVED | REJECTED | AUTO_ROUTED | CANCELLED | DATA_ACTION_EXECUTED */
    private String action;
    private String performedBy;
    private Instant performedAt = Instant.now();
    private Map<String, Object> data = new HashMap<>();
    private String comment;

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public Instant getPerformedAt() { return performedAt; }
    public void setPerformedAt(Instant performedAt) { this.performedAt = performedAt; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
