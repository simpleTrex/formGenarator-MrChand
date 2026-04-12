package com.adaptivebp.modules.process.dto;

import com.adaptivebp.modules.process.model.ProcessInstance;
import com.adaptivebp.modules.process.model.ProcessNode;

public class ProcessInstanceResponse {

    private ProcessInstance instance;
    /** The resolved current node config — tells the frontend what to render */
    private ProcessNode currentNode;

    public static ProcessInstanceResponse of(ProcessInstance instance, ProcessNode currentNode) {
        ProcessInstanceResponse r = new ProcessInstanceResponse();
        r.instance = instance;
        r.currentNode = currentNode;
        return r;
    }

    public ProcessInstance getInstance() { return instance; }
    public ProcessNode getCurrentNode() { return currentNode; }
}
