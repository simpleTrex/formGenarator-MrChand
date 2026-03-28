package com.adaptivebp.modules.workflow.model;

import java.util.ArrayList;
import java.util.List;

import com.adaptivebp.modules.formbuilder.model.DomainModelField;

public class WorkflowStep {
    private String id;
    private String modelId;
    private String name;
    private int order;
    private boolean isStart;
    private boolean isEnd;
    private List<DomainModelField> fields = new ArrayList<>();
    private List<WorkflowEdge> edges = new ArrayList<>();
    private StepDataConfig dataConfig;
    private Double positionX;
    private Double positionY;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public List<DomainModelField> getFields() {
        return fields;
    }

    public void setFields(List<DomainModelField> fields) {
        this.fields = fields != null ? fields : new ArrayList<>();
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

    public StepDataConfig getDataConfig() {
        return dataConfig;
    }

    public void setDataConfig(StepDataConfig dataConfig) {
        this.dataConfig = dataConfig;
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
