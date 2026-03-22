package com.adaptivebp.modules.process.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.adaptivebp.modules.process.model.ProcessInstance;
import com.adaptivebp.modules.process.model.enums.InstanceStatus;

public interface ProcessInstanceRepository extends MongoRepository<ProcessInstance, String> {

    List<ProcessInstance> findByDomainIdAndAppId(String domainId, String appId);

    List<ProcessInstance> findByDomainIdAndAppIdAndStatus(
            String domainId, String appId, InstanceStatus status);

    List<ProcessInstance> findByAssignedToUserIdAndStatus(String userId, InstanceStatus status);

    long countByProcessDefinitionIdAndStatus(String definitionId, InstanceStatus status);
}
