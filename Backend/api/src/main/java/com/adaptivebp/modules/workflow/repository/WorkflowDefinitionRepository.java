package com.adaptivebp.modules.workflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.adaptivebp.modules.workflow.model.WorkflowDefinition;
import com.adaptivebp.modules.workflow.model.enums.WorkflowStatus;

public interface WorkflowDefinitionRepository extends MongoRepository<WorkflowDefinition, String> {
    Optional<WorkflowDefinition> findByDomainIdAndAppIdAndSlugAndStatus(
            String domainId, String appId, String slug, WorkflowStatus status);

    List<WorkflowDefinition> findByDomainIdAndAppId(String domainId, String appId);

    Optional<WorkflowDefinition> findTopByDomainIdAndAppIdAndSlugOrderByVersionDesc(
            String domainId, String appId, String slug);

    Optional<WorkflowDefinition> findTopByDomainIdAndAppIdOrderByVersionDesc(String domainId, String appId);

    boolean existsByDomainIdAndAppIdAndSlug(String domainId, String appId, String slug);

    void deleteByDomainIdAndAppId(String domainId, String appId);
}
