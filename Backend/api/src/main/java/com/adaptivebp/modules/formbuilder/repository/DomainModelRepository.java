package com.adaptivebp.modules.formbuilder.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.adaptivebp.modules.formbuilder.model.DomainModel;

public interface DomainModelRepository extends MongoRepository<DomainModel, String> {
    List<DomainModel> findByDomainId(String domainId);
    Optional<DomainModel> findByDomainIdAndSlug(String domainId, String slug);
    boolean existsByDomainIdAndSlug(String domainId, String slug);
}
