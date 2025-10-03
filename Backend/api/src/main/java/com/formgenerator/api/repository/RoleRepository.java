package com.formgenerator.api.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.formgenerator.platform.auth.ERole;
import com.formgenerator.platform.auth.Role;

public interface RoleRepository extends MongoRepository<Role, String> {
	Optional<Role> findByName(ERole name);
	Optional<Role> findByRoleName(String name);
}