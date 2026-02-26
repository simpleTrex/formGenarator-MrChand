package com.adaptivebp.modules.appmanagement.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.adaptivebp.modules.appmanagement.model.Application;
import com.adaptivebp.modules.appmanagement.port.ApplicationLookupPort;
import com.adaptivebp.modules.appmanagement.repository.ApplicationRepository;

/**
 * Implements ApplicationLookupPort — the public read API for application lookups.
 * Cross-module callers (e.g. formbuilder's DomainModelController) inject this
 * via the port interface, never touching ApplicationRepository directly.
 */
@Service
public class ApplicationQueryService implements ApplicationLookupPort {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    public Optional<Application> findByDomainIdAndSlug(String domainId, String appSlug) {
        return applicationRepository.findByDomainIdAndSlug(domainId, appSlug);
    }
}
