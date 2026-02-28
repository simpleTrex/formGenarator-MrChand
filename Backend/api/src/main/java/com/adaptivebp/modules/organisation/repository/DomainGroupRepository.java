package com.adaptivebp.modules.organisation.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.adaptivebp.modules.organisation.model.DomainGroup;

public interface DomainGroupRepository extends MongoRepository<DomainGroup, String> {
    List<DomainGroup> findByDomainId(String domainId);
    Optional<DomainGroup> findByDomainIdAndName(String domainId, String name);
}
