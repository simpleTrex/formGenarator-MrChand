package com.adaptivebp.modules.appmanagement.port;

import java.util.Optional;

import com.adaptivebp.modules.appmanagement.model.Application;

/**
 * Public API that the appmanagement module exposes for application lookups.
 * Other modules (e.g. formbuilder's DomainModelController) must depend on this
 * interface — never on ApplicationRepository directly.
 */
public interface ApplicationLookupPort {
    Optional<Application> findByDomainIdAndSlug(String domainId, String appSlug);
}
