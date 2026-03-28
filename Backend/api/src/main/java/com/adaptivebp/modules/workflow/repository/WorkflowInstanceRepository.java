package com.adaptivebp.modules.workflow.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.adaptivebp.modules.workflow.model.WorkflowInstance;
import com.adaptivebp.modules.workflow.model.enums.InstanceStatus;

public interface WorkflowInstanceRepository extends MongoRepository<WorkflowInstance, String> {
    List<WorkflowInstance> findByDomainIdAndAppIdAndStatus(String domainId, String appId, InstanceStatus status);

    List<WorkflowInstance> findByDomainIdAndAppId(String domainId, String appId);

    List<WorkflowInstance> findByDomainIdAndAppIdAndStartedBy(String domainId, String appId, String startedBy);

    List<WorkflowInstance> findByStartedByAndStatus(String userId, InstanceStatus status);

    long countByWorkflowDefinitionIdAndStatus(String definitionId, InstanceStatus status);

    void deleteByDomainIdAndAppId(String domainId, String appId);
}
