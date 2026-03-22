package com.adaptivebp.modules.process.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.adaptivebp.modules.process.model.ProcessDefinition;
import com.adaptivebp.modules.process.model.enums.ProcessStatus;

public interface ProcessDefinitionRepository extends MongoRepository<ProcessDefinition, String> {

    Optional<ProcessDefinition> findByDomainIdAndAppIdAndSlugAndStatus(
            String domainId, String appId, String slug, ProcessStatus status);

    Optional<ProcessDefinition> findByDomainIdAndAppIdAndSlugAndVersion(
            String domainId, String appId, String slug, int version);

    List<ProcessDefinition> findByDomainIdAndAppId(String domainId, String appId);

    Optional<ProcessDefinition> findTopByDomainIdAndAppIdOrderByVersionDesc(String domainId, String appId);

    Optional<ProcessDefinition> findTopByDomainIdAndAppIdAndSlugOrderByVersionDesc(
            String domainId, String appId, String slug);

    boolean existsByDomainIdAndAppIdAndSlug(String domainId, String appId, String slug);

    List<ProcessDefinition> findByDomainIdAndAppIdAndStatus(
            String domainId, String appId, ProcessStatus status);
}
