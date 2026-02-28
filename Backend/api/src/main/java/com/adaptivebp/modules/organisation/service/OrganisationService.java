package com.adaptivebp.modules.organisation.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.adaptivebp.modules.organisation.model.Organisation;
import com.adaptivebp.modules.organisation.port.OrganisationLookupPort;
import com.adaptivebp.modules.organisation.repository.OrganisationRepository;

/**
 * Implements OrganisationLookupPort — the public read API for the organisation module.
 * Wraps OrganisationRepository so that cross-module callers never import the repo directly.
 */
@Service
public class OrganisationService implements OrganisationLookupPort {

    @Autowired
    private OrganisationRepository organisationRepository;

    @Override
    public Optional<Organisation> findBySlug(String slug) {
        return organisationRepository.findBySlug(slug);
    }
}
