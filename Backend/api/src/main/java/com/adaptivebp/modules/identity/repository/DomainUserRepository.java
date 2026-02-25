package com.adaptivebp.modules.identity.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.adaptivebp.modules.identity.model.DomainUser;

public interface DomainUserRepository extends MongoRepository<DomainUser, String> {
    Optional<DomainUser> findByDomainIdAndUsername(String domainId, String username);
    Optional<DomainUser> findByDomainIdAndEmail(String domainId, String email);
    List<DomainUser> findByDomainId(String domainId);
}
