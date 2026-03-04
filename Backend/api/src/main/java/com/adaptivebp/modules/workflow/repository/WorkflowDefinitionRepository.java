package com.adaptivebp.modules.workflow.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.adaptivebp.modules.workflow.model.WorkflowDefinition;

import java.util.List;
import java.util.Optional;

/**
 * Repository for WorkflowDefinition entities
 */
@Repository
public interface WorkflowDefinitionRepository extends MongoRepository<WorkflowDefinition, String> {

    List<WorkflowDefinition> findByDomainId(String domainId);

    List<WorkflowDefinition> findByDomainIdAndIsActive(String domainId, boolean isActive);

    Optional<WorkflowDefinition> findByIdAndDomainId(String id, String domainId);

    List<WorkflowDefinition> findByModelId(String modelId);
}
