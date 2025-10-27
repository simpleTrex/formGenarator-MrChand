package com.formgenerator.platform.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DomainSecurityTest {

    private final DomainSecurity domainSecurity = new DomainSecurity();

    @Test
    void isSameDomain_shouldReturnTrue_whenIdsMatch() {
        assertTrue(domainSecurity.isSameDomain("abc123", "abc123"));
        assertTrue(domainSecurity.isSameDomain(" abc123 ", "abc123"));
    }

    @Test
    void isSameDomain_shouldReturnFalse_whenIdsDifferOrEmpty() {
        assertFalse(domainSecurity.isSameDomain("abc123", "def456"));
        assertFalse(domainSecurity.isSameDomain(null, "abc123"));
        assertFalse(domainSecurity.isSameDomain("abc123", null));
        assertFalse(domainSecurity.isSameDomain("", "abc123"));
        assertFalse(domainSecurity.isSameDomain("abc123", ""));
    }
}
