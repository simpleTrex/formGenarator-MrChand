package com.adaptivebp.modules.process.dto;

import com.adaptivebp.modules.process.model.ProcessDefinition;

public class ProcessDefinitionResponse {

    private ProcessDefinition definition;
    private int nodeCount;
    private int edgeCount;
    private boolean valid;

    public static ProcessDefinitionResponse of(ProcessDefinition def, boolean valid) {
        ProcessDefinitionResponse r = new ProcessDefinitionResponse();
        r.definition = def;
        r.nodeCount = def.getNodes() == null ? 0 : def.getNodes().size();
        r.edgeCount = def.getEdges() == null ? 0 : def.getEdges().size();
        r.valid = valid;
        return r;
    }

    public ProcessDefinition getDefinition() { return definition; }
    public int getNodeCount() { return nodeCount; }
    public int getEdgeCount() { return edgeCount; }
    public boolean isValid() { return valid; }
}
