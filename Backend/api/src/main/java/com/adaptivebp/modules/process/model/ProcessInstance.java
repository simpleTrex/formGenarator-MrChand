package com.adaptivebp.modules.process.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.adaptivebp.modules.process.model.embedded.Assignment;
import com.adaptivebp.modules.process.model.embedded.CreatedRecord;
import com.adaptivebp.modules.process.model.embedded.HistoryEntry;
import com.adaptivebp.modules.process.model.enums.InstanceStatus;

@Document(collection = "process_instances")
@CompoundIndexes({
        @CompoundIndex(name = "domain_app_status_idx", def = "{'domainId':1,'appId':1,'status':1}"),
        @CompoundIndex(name = "assigned_status_idx", def = "{'assignedTo.userId':1,'status':1}")
})
public class ProcessInstance {

    @Id
    private String id;

    @Indexed
    private String processDefinitionId;
    private int processVersion;
    private String domainId;
    private String appId;

    private InstanceStatus status = InstanceStatus.ACTIVE;
    private String currentNodeId;
    private String previousNodeId;

    /** key = "nodeId.elementId", value = user input */
    private Map<String, Object> data = new HashMap<>();

    private List<CreatedRecord> createdRecordIds = new ArrayList<>();
    private Assignment assignedTo;

    /** Append-only audit trail — never mutate existing entries */
    private List<HistoryEntry> history = new ArrayList<>();

    private String startedBy;
    private Instant startedAt = Instant.now();
    private Instant completedAt;

    /** Unsaved draft data for FORM_PAGE nodes (if allowSaveDraft=true) */
    private Map<String, Object> draftData = new HashMap<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProcessDefinitionId() { return processDefinitionId; }
    public void setProcessDefinitionId(String processDefinitionId) { this.processDefinitionId = processDefinitionId; }

    public int getProcessVersion() { return processVersion; }
    public void setProcessVersion(int processVersion) { this.processVersion = processVersion; }

    public String getDomainId() { return domainId; }
    public void setDomainId(String domainId) { this.domainId = domainId; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public InstanceStatus getStatus() { return status; }
    public void setStatus(InstanceStatus status) { this.status = status; }

    public String getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(String currentNodeId) { this.currentNodeId = currentNodeId; }

    public String getPreviousNodeId() { return previousNodeId; }
    public void setPreviousNodeId(String previousNodeId) { this.previousNodeId = previousNodeId; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    public List<CreatedRecord> getCreatedRecordIds() { return createdRecordIds; }
    public void setCreatedRecordIds(List<CreatedRecord> createdRecordIds) { this.createdRecordIds = createdRecordIds; }

    public Assignment getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Assignment assignedTo) { this.assignedTo = assignedTo; }

    public List<HistoryEntry> getHistory() { return history; }
    public void setHistory(List<HistoryEntry> history) { this.history = history; }

    public String getStartedBy() { return startedBy; }
    public void setStartedBy(String startedBy) { this.startedBy = startedBy; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Map<String, Object> getDraftData() { return draftData; }
    public void setDraftData(Map<String, Object> draftData) { this.draftData = draftData; }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public void appendHistory(HistoryEntry entry) {
        this.history.add(entry);
    }
}
