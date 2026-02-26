package com.adaptivebp.modules.identity.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.adaptivebp.modules.identity.model.DomainUser;
import com.adaptivebp.modules.identity.port.DomainUserLookupPort;
import com.adaptivebp.modules.identity.repository.DomainUserRepository;

/**
 * Implements DomainUserLookupPort — the public read API for domain-user lookups.
 * Cross-module callers (e.g. DomainGroupController in organisation,
 * AppGroupController in appmanagement) inject this via the port interface,
 * never touching DomainUserRepository directly.
 */
@Service
public class DomainUserService implements DomainUserLookupPort {

    @Autowired
    private DomainUserRepository domainUserRepository;

    @Override
    public Optional<DomainUser> findById(String id) {
        return domainUserRepository.findById(id);
    }

    @Override
    public Optional<DomainUser> findByDomainIdAndUsername(String domainId, String username) {
        return domainUserRepository.findByDomainIdAndUsername(domainId, username);
    }

    @Override
    public List<DomainUser> findByDomainId(String domainId) {
        return domainUserRepository.findByDomainId(domainId);
    }
}
