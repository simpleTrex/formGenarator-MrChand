package com.formgenerator.platform.workflow;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for WorkflowInstance entities
 */
@Repository
public interface WorkflowInstanceRepository extends MongoRepository<WorkflowInstance, String> {
    
    List<WorkflowInstance> findByDomainId(String domainId);
    
    List<WorkflowInstance> findByWorkflowDefinitionId(String workflowDefinitionId);
    
    List<WorkflowInstance> findByCurrentState(String currentState);
    
    List<WorkflowInstance> findByDomainIdAndCurrentState(String domainId, String currentState);
    
    Optional<WorkflowInstance> findByRecordId(String recordId);
    
    List<WorkflowInstance> findByAssignedTo_UserId(String userId);
    
    Optional<WorkflowInstance> findByIdAndDomainId(String id, String domainId);
}
