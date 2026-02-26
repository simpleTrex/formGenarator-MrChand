package com.adaptivebp.modules.identity.port;

import java.util.List;
import java.util.Optional;

import com.adaptivebp.modules.identity.model.DomainUser;

/**
 * Public API that the identity module exposes for domain-user lookups.
 * Other modules (e.g. organisation's DomainGroupController, appmanagement's
 * AppGroupController) must depend on this interface — never on
 * DomainUserRepository directly.
 */
public interface DomainUserLookupPort {
    Optional<DomainUser> findById(String id);
    Optional<DomainUser> findByDomainIdAndUsername(String domainId, String username);
    List<DomainUser> findByDomainId(String domainId);
}
