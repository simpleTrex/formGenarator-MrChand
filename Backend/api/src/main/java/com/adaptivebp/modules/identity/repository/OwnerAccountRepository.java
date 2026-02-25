package com.adaptivebp.modules.identity.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.adaptivebp.modules.identity.model.OwnerAccount;

public interface OwnerAccountRepository extends MongoRepository<OwnerAccount, String> {
    Optional<OwnerAccount> findByEmail(String email);
    boolean existsByEmail(String email);
}
