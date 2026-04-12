package com.adaptivebp.modules.process.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.adaptivebp.modules.process.model.enums.ProcessStatus;

@Document(collection = "process_definitions")
@CompoundIndexes({
        @CompoundIndex(name = "domain_app_slug_version_idx",
                def = "{'domainId':1,'appId':1,'slug':1,'version':1}", unique = true)
})
public class ProcessDefinition {

    @Id
    private String id;
    private String domainId;
    private String appId;
    private String name;
    private String slug;
    private String description;
    private int version = 1;
    private ProcessStatus status = ProcessStatus.DRAFT;
    private List<String> linkedModelIds = new ArrayList<>();
    private List<ProcessNode> nodes = new ArrayList<>();
    private List<ProcessEdge> edges = new ArrayList<>();
    private ProcessSettings settings = new ProcessSettings();
    private String createdBy;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDomainId() { return domainId; }
    public void setDomainId(String domainId) { this.domainId = domainId; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public ProcessStatus getStatus() { return status; }
    public void setStatus(ProcessStatus status) { this.status = status; }

    public List<String> getLinkedModelIds() { return linkedModelIds; }
    public void setLinkedModelIds(List<String> linkedModelIds) { this.linkedModelIds = linkedModelIds; }

    public List<ProcessNode> getNodes() { return nodes; }
    public void setNodes(List<ProcessNode> nodes) { this.nodes = nodes; }

    public List<ProcessEdge> getEdges() { return edges; }
    public void setEdges(List<ProcessEdge> edges) { this.edges = edges; }

    public ProcessSettings getSettings() { return settings; }
    public void setSettings(ProcessSettings settings) { this.settings = settings; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public ProcessNode findNodeById(String nodeId) {
        if (nodeId == null || nodes == null) return null;
        return nodes.stream().filter(n -> nodeId.equals(n.getId())).findFirst().orElse(null);
    }

    public ProcessNode findStartNode() {
        if (nodes == null) return null;
        return nodes.stream()
                .filter(n -> n.getType() != null &&
                        n.getType() == com.adaptivebp.modules.process.model.enums.NodeType.START)
                .findFirst().orElse(null);
    }

    public List<ProcessEdge> outgoingEdges(String fromNodeId) {
        List<ProcessEdge> result = new ArrayList<>();
        if (edges == null || fromNodeId == null) return result;
        for (ProcessEdge e : edges) {
            if (fromNodeId.equals(e.getFromNodeId())) result.add(e);
        }
        return result;
    }

    public List<ProcessEdge> incomingEdges(String toNodeId) {
        List<ProcessEdge> result = new ArrayList<>();
        if (edges == null || toNodeId == null) return result;
        for (ProcessEdge e : edges) {
            if (toNodeId.equals(e.getToNodeId())) result.add(e);
        }
        return result;
    }
}
