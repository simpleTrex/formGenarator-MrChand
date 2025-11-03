package com.formgenerator.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.formgenerator.platform.auth.Domain;

/**
 * Repository interface for Domain entity operations.
 * Extends MongoRepository to provide basic CRUD operations.
 */
public interface DomainRepository extends MongoRepository<Domain, String> {
    
    /**
     * Find domain by name (case-sensitive).
     * @param name the domain name
     * @return Optional containing the domain if found
     */
    Optional<Domain> findByName(String name);
    
    /**
     * Check if domain name already exists.
     * @param name the domain name to check
     * @return true if domain name exists, false otherwise
     */
    Boolean existsByName(String name);
    
    /**
     * Find all domains owned by a specific user.
     * @param ownerUserId the owner's user ID
     * @return list of domains owned by the user
     */
    List<Domain> findByOwnerUserId(String ownerUserId);

    /**
     * Find domain by URL slug (unique).
     */
    Optional<Domain> findBySlug(String slug);

    /**
     * Check if a slug already exists.
     */
    Boolean existsBySlug(String slug);
}