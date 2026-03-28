package com.adaptivebp.modules.appmanagement.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.adaptivebp.modules.appmanagement.model.Application;
import com.adaptivebp.modules.appmanagement.repository.AppGroupMemberRepository;
import com.adaptivebp.modules.appmanagement.repository.AppGroupRepository;
import com.adaptivebp.modules.appmanagement.repository.ApplicationRepository;
import com.adaptivebp.modules.formbuilder.repository.ModelRecordRepository;
import com.adaptivebp.modules.formbuilder.model.DomainModel;
import com.adaptivebp.modules.formbuilder.repository.DomainModelRepository;
import com.adaptivebp.modules.process.repository.ProcessDefinitionRepository;
import com.adaptivebp.modules.process.repository.ProcessInstanceRepository;
import com.adaptivebp.modules.workflow.repository.WorkflowDefinitionRepository;
import com.adaptivebp.modules.workflow.repository.WorkflowInstanceRepository;

@Service
public class ApplicationDeletionService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationDeletionService.class);

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private AppGroupRepository appGroupRepository;

    @Autowired
    private AppGroupMemberRepository appGroupMemberRepository;

    @Autowired
    private ProcessDefinitionRepository processDefinitionRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private WorkflowDefinitionRepository workflowDefinitionRepository;

    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;

    @Autowired
    private ModelRecordRepository modelRecordRepository;

    @Autowired
    private DomainModelRepository domainModelRepository;

    public void deleteApplication(String domainId, Application application) {
        if (domainId == null || application == null || application.getId() == null) {
            return;
        }
        String appId = application.getId();

        // Children first
        safeDelete("modelRecords", () -> modelRecordRepository.deleteByDomainIdAndAppId(domainId, appId));
        safeDelete("processInstances", () -> processInstanceRepository.deleteByDomainIdAndAppId(domainId, appId));
        safeDelete("processDefinitions", () -> processDefinitionRepository.deleteByDomainIdAndAppId(domainId, appId));
        safeDelete("workflowInstances", () -> workflowInstanceRepository.deleteByDomainIdAndAppId(domainId, appId));
        safeDelete("workflowDefinitions", () -> workflowDefinitionRepository.deleteByDomainIdAndAppId(domainId, appId));

        safeDelete("domainModels", () -> removeAppFromDomainModels(domainId, appId));
        safeDelete("appGroupMembers", () -> appGroupMemberRepository.deleteByAppId(appId));
        safeDelete("appGroups", () -> appGroupRepository.deleteByAppId(appId));
        applicationRepository.deleteById(appId);
    }

    private void removeAppFromDomainModels(String domainId, String appId) {
        List<DomainModel> models = domainModelRepository.findByDomainId(domainId);
        for (DomainModel model : models) {
            if (model.getAllowedAppIds() != null && model.getAllowedAppIds().contains(appId)) {
                model.getAllowedAppIds().remove(appId);
                domainModelRepository.save(model);
            }
        }
    }

    private void safeDelete(String label, Runnable action) {
        try {
            action.run();
        } catch (Exception ex) {
            log.warn("Failed to delete {} during application cleanup", label, ex);
        }
    }
}