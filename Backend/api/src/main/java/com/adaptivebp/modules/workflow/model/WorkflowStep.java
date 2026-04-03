package com.adaptivebp.modules.workflow.model;

import java.util.ArrayList;
import java.util.List;

import com.adaptivebp.modules.formbuilder.model.DomainModelField;

public class WorkflowStep {
    private String id;
    private String name;
    private int order;
    private boolean isStart;
    private boolean isEnd;
    /** Fields editable at this step (added at this step for the first time). */
    private List<DomainModelField> fields = new ArrayList<>();
    /**
     * Keys of fields from previous steps that are shown read-only at this step
     * for context. The actual field definitions are looked up across all steps.
     */
    private List<String> readonlyFieldKeys = new ArrayList<>();
    private StepDataConfig dataConfig;
    private List<WorkflowEdge> edges = new ArrayList<>();
    private Double positionX;
    private Double positionY;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<DomainModelField> getFields() {
        return fields;
    }

    public void setFields(List<DomainModelField> fields) {
        this.fields = fields != null ? fields : new ArrayList<>();
    }

    public List<String> getReadonlyFieldKeys() {
        return readonlyFieldKeys;
    }

    public void setReadonlyFieldKeys(List<String> readonlyFieldKeys) {
        this.readonlyFieldKeys = readonlyFieldKeys != null ? readonlyFieldKeys : new ArrayList<>();
    }

    public StepDataConfig getDataConfig() {
        return dataConfig;
    }

    public void setDataConfig(StepDataConfig dataConfig) {
        this.dataConfig = dataConfig;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isStart() {
        return isStart;
    }

    public void setStart(boolean start) {
        isStart = start;
    }

    public boolean isEnd() {
        return isEnd;
    }

    public void setEnd(boolean end) {
        isEnd = end;
    }

    public List<WorkflowEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<WorkflowEdge> edges) {
        this.edges = edges != null ? edges : new ArrayList<>();
    }

    public Double getPositionX() {
        return positionX;
    }

    public void setPositionX(Double positionX) {
        this.positionX = positionX;
    }

    public Double getPositionY() {
        return positionY;
    }

    public void setPositionY(Double positionY) {
        this.positionY = positionY;
    }
}
