package com.formgenerator.platform.auth;

public class JwtPrincipalClaims {

    private final String principalId;
    private final PrincipalType principalType;
    private final String domainId;
    private final String username;

    public JwtPrincipalClaims(String principalId, PrincipalType principalType, String domainId, String username) {
        this.principalId = principalId;
        this.principalType = principalType;
        this.domainId = domainId;
        this.username = username;
    }

    public String getPrincipalId() {
        return principalId;
    }

    public PrincipalType getPrincipalType() {
        return principalType;
    }

    public String getDomainId() {
        return domainId;
    }

    public String getUsername() {
        return username;
    }
}
