package com.adaptivebp.modules.appmanagement.permission;

public enum AppPermission {
    APP_VIEW,
    APP_CONFIGURE,
    APP_MANAGE_WORKFLOW,
    APP_START_WORKFLOW,
    APP_EXECUTE_WORKFLOW,
    APP_VIEW_ALL_INSTANCES,

    // Legacy permissions kept temporarily for backward compatibility.
    APP_READ,
    APP_WRITE,
    APP_EXECUTE,
    APP_MANAGE_PROCESSES,
    APP_START_PROCESS,
    APP_VIEW_PROCESSES,
    APP_MANAGE_WORKFLOWS,
    APP_VIEW_WORKFLOWS
}
