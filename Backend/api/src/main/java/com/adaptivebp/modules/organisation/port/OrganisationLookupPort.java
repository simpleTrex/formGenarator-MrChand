package com.adaptivebp.modules.organisation.port;

import java.util.Optional;

import com.adaptivebp.modules.organisation.model.Organisation;

/**
 * Public API that the organisation module exposes for read-only lookups.
 * Other modules must depend on this interface — never on OrganisationRepository directly.
 */
public interface OrganisationLookupPort {
    Optional<Organisation> findBySlug(String slug);
}
