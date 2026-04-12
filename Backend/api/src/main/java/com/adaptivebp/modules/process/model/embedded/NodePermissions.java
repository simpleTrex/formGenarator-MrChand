package com.adaptivebp.modules.process.model.embedded;

import java.util.ArrayList;
import java.util.List;

public class NodePermissions {

    private List<String> allowedRoles = new ArrayList<>();
    private List<String> allowedUserIds = new ArrayList<>();

    public List<String> getAllowedRoles() { return allowedRoles; }
    public void setAllowedRoles(List<String> allowedRoles) { this.allowedRoles = allowedRoles; }

    public List<String> getAllowedUserIds() { return allowedUserIds; }
    public void setAllowedUserIds(List<String> allowedUserIds) { this.allowedUserIds = allowedUserIds; }
}
