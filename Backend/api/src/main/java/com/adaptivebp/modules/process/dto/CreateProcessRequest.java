package com.adaptivebp.modules.process.dto;

import java.util.ArrayList;
import java.util.List;

import com.adaptivebp.modules.process.model.ProcessEdge;
import com.adaptivebp.modules.process.model.ProcessNode;
import com.adaptivebp.modules.process.model.ProcessSettings;

import jakarta.validation.constraints.NotBlank;

public class CreateProcessRequest {

    @NotBlank(message = "name is required")
    private String name;

    private String description;
    private List<ProcessNode> nodes = new ArrayList<>();
    private List<ProcessEdge> edges = new ArrayList<>();
    private ProcessSettings settings = new ProcessSettings();
    private List<String> linkedModelIds = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<ProcessNode> getNodes() { return nodes; }
    public void setNodes(List<ProcessNode> nodes) { this.nodes = nodes; }

    public List<ProcessEdge> getEdges() { return edges; }
    public void setEdges(List<ProcessEdge> edges) { this.edges = edges; }

    public ProcessSettings getSettings() { return settings; }
    public void setSettings(ProcessSettings settings) { this.settings = settings; }

    public List<String> getLinkedModelIds() { return linkedModelIds; }
    public void setLinkedModelIds(List<String> linkedModelIds) { this.linkedModelIds = linkedModelIds; }
}
