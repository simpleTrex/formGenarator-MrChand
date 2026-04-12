package com.adaptivebp.modules.workflow.model;

import java.util.ArrayList;
import java.util.List;

public class StepDataConfig {
    private boolean referencePreviousStep;
    private String reuseFromStepId;
    private List<String> previousStepFields = new ArrayList<>();
    private List<AutoFetchRule> autoFetchRules = new ArrayList<>();
    private List<String> readOnlyFields = new ArrayList<>();

    public boolean isReferencePreviousStep() {
        return referencePreviousStep;
    }

    public void setReferencePreviousStep(boolean referencePreviousStep) {
        this.referencePreviousStep = referencePreviousStep;
    }

    public String getReuseFromStepId() {
        return reuseFromStepId;
    }

    public void setReuseFromStepId(String reuseFromStepId) {
        this.reuseFromStepId = reuseFromStepId;
    }

    public List<String> getPreviousStepFields() {
        return previousStepFields;
    }

    public void setPreviousStepFields(List<String> previousStepFields) {
        this.previousStepFields = previousStepFields != null ? previousStepFields : new ArrayList<>();
    }

    public List<AutoFetchRule> getAutoFetchRules() {
        return autoFetchRules;
    }

    public void setAutoFetchRules(List<AutoFetchRule> autoFetchRules) {
        this.autoFetchRules = autoFetchRules != null ? autoFetchRules : new ArrayList<>();
    }

    public List<String> getReadOnlyFields() {
        return readOnlyFields;
    }

    public void setReadOnlyFields(List<String> readOnlyFields) {
        this.readOnlyFields = readOnlyFields != null ? readOnlyFields : new ArrayList<>();
    }
}
