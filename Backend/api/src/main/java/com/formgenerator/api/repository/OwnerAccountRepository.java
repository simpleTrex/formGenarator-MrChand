package com.formgenerator.api.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.formgenerator.api.models.owner.OwnerAccount;

public interface OwnerAccountRepository extends MongoRepository<OwnerAccount, String> {

    Optional<OwnerAccount> findByEmail(String email);

    boolean existsByEmail(String email);
}
