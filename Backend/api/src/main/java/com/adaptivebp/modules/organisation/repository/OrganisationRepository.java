package com.adaptivebp.modules.organisation.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.adaptivebp.modules.organisation.model.Organisation;

public interface OrganisationRepository extends MongoRepository<Organisation, String> {
    Optional<Organisation> findByName(String name);
    Boolean existsByName(String name);
    List<Organisation> findByOwnerUserId(String ownerUserId);
    Optional<Organisation> findBySlug(String slug);
    Boolean existsBySlug(String slug);
}
