package com.adaptivebp.modules.process.model;

public class ProcessSettings {

    private boolean allowSaveDraft = false;
    private boolean requireAuth = true;

    public boolean isAllowSaveDraft() { return allowSaveDraft; }
    public void setAllowSaveDraft(boolean allowSaveDraft) { this.allowSaveDraft = allowSaveDraft; }

    public boolean isRequireAuth() { return requireAuth; }
    public void setRequireAuth(boolean requireAuth) { this.requireAuth = requireAuth; }
}
