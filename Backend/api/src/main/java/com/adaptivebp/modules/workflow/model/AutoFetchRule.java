package com.adaptivebp.modules.workflow.model;

import java.util.ArrayList;
import java.util.List;

public class AutoFetchRule {
    private String sourceStepId;
    private String sourceField;
    private String targetField;

    public String getSourceStepId() {
        return sourceStepId;
    }

    public void setSourceStepId(String sourceStepId) {
        this.sourceStepId = sourceStepId;
    }

    public String getSourceField() {
        return sourceField;
    }

    public void setSourceField(String sourceField) {
        this.sourceField = sourceField;
    }

    public String getTargetField() {
        return targetField;
    }

    public void setTargetField(String targetField) {
        this.targetField = targetField;
    }
}
