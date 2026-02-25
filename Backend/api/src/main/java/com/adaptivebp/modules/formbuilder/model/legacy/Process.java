package com.adaptivebp.modules.formbuilder.model.legacy;

import java.util.List;

public class Process {
    private FormField action;
    private ModelAuth mAuth;
    private DataAuth dAuth;
    private ModelStatus mStatus;
    private DataStatus dStatus;
    private List<CustomForm> objects;

    public List<CustomForm> getObjects() { return objects; }
    public void setObjects(List<CustomForm> objects) { this.objects = objects; }
    public FormField getAction() { return action; }
    public void setAction(FormField action) { this.action = action; }
    public ModelAuth getmAuth() { return mAuth; }
    public void setmAuth(ModelAuth mAuth) { this.mAuth = mAuth; }
    public DataAuth getdAuth() { return dAuth; }
    public void setdAuth(DataAuth dAuth) { this.dAuth = dAuth; }
    public ModelStatus getmStatus() { return mStatus; }
    public void setmStatus(ModelStatus mStatus) { this.mStatus = mStatus; }
    public DataStatus getdStatus() { return dStatus; }
    public void setdStatus(DataStatus dStatus) { this.dStatus = dStatus; }
}
