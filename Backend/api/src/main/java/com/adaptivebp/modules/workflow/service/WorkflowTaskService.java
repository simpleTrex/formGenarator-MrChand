package com.adaptivebp.modules.workflow.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.adaptivebp.modules.workflow.dto.response.TaskListResponse;

@Service
public class WorkflowTaskService {

    @Autowired
    private WorkflowEngineService workflowEngineService;

    public TaskListResponse getMyTasks(String userId, String domainId, String appId) {
        return workflowEngineService.getMyTasks(userId, domainId, appId);
    }
}
