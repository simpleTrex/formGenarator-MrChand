package com.adaptivebp.modules.identity.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.adaptivebp.modules.identity.model.ERole;
import com.adaptivebp.modules.identity.model.Role;

public interface RoleRepository extends MongoRepository<Role, String> {
	Optional<Role> findByName(ERole name);
	Optional<Role> findByRoleName(String name);
}
