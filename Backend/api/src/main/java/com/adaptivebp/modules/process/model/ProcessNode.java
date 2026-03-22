package com.adaptivebp.modules.process.model;

import java.util.HashMap;
import java.util.Map;

import com.adaptivebp.modules.process.model.embedded.NodePermissions;
import com.adaptivebp.modules.process.model.enums.NodeType;

public class ProcessNode {

    /** Unique within this process definition, e.g. "node_1" */
    private String id;
    private NodeType type;
    private String name;
    private Double positionX;
    private Double positionY;
    /** Flexible config — parsed by the engine based on NodeType */
    private Map<String, Object> config = new HashMap<>();
    private NodePermissions permissions = new NodePermissions();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public NodeType getType() { return type; }
    public void setType(NodeType type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getPositionX() { return positionX; }
    public void setPositionX(Double positionX) { this.positionX = positionX; }

    public Double getPositionY() { return positionY; }
    public void setPositionY(Double positionY) { this.positionY = positionY; }

    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }

    public NodePermissions getPermissions() { return permissions; }
    public void setPermissions(NodePermissions permissions) { this.permissions = permissions; }
}
