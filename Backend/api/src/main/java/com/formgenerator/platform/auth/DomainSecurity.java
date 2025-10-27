package com.formgenerator.platform.auth;

import org.springframework.stereotype.Component;

/**
 * Helper bean used in @PreAuthorize expressions to enforce domain-based access.
 */
@Component("domainSecurity")
public class DomainSecurity {

    /**
     * Returns true if the resource's domainId matches the authenticated principal's domainId.
     * Null-safe and trims input.
     */
    public boolean isSameDomain(String resourceDomainId, String principalDomainId) {
        if (resourceDomainId == null || principalDomainId == null) return false;
        String a = resourceDomainId.trim();
        String b = principalDomainId.trim();
        if (a.isEmpty() || b.isEmpty()) return false;
        return a.equals(b);
    }
}
