package com.adaptivebp.modules.workflow.model;

import java.util.HashMap;
import java.util.Map;

import com.adaptivebp.modules.workflow.model.enums.AutoActionType;

public class AutoAction {
    private AutoActionType type;
    private Map<String, Object> config = new HashMap<>();

    public AutoActionType getType() {
        return type;
    }

    public void setType(AutoActionType type) {
        this.type = type;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config != null ? config : new HashMap<>();
    }
}
