package com.adaptivebp.modules.uibuilder.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.adaptivebp.modules.uibuilder.model.ComponentDefinition;

public interface ComponentDefinitionRepository extends MongoRepository<ComponentDefinition, String> {
    List<ComponentDefinition> findByAppId(String appId);

    List<ComponentDefinition> findByAppIdAndDomainSlug(String appId, String domainSlug);
}
