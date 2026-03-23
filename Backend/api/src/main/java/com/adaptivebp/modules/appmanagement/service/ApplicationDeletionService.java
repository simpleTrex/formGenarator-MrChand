package com.adaptivebp.modules.appmanagement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.adaptivebp.modules.appmanagement.model.Application;
import com.adaptivebp.modules.appmanagement.repository.AppGroupMemberRepository;
import com.adaptivebp.modules.appmanagement.repository.AppGroupRepository;
import com.adaptivebp.modules.appmanagement.repository.ApplicationRepository;
import com.adaptivebp.modules.formbuilder.repository.ModelRecordRepository;
import com.adaptivebp.modules.process.repository.ProcessDefinitionRepository;
import com.adaptivebp.modules.process.repository.ProcessInstanceRepository;

@Service
public class ApplicationDeletionService {

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
    private ModelRecordRepository modelRecordRepository;

    public void deleteApplication(String domainId, Application application) {
        if (domainId == null || application == null || application.getId() == null) {
            return;
        }
        String appId = application.getId();

        // Children first
        modelRecordRepository.deleteByDomainIdAndAppId(domainId, appId);
        processInstanceRepository.deleteByDomainIdAndAppId(domainId, appId);
        processDefinitionRepository.deleteByDomainIdAndAppId(domainId, appId);
        appGroupMemberRepository.deleteByAppId(appId);
        appGroupRepository.deleteByAppId(appId);
        applicationRepository.deleteById(appId);
    }
}