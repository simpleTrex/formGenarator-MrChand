package com.formgenerator.api.dto.auth;

import com.formgenerator.platform.auth.PrincipalType;

public class AuthResponse {

    private String userId;
    private String domainId;
    private PrincipalType principalType;
    private String token;

    public AuthResponse(String userId, String domainId, PrincipalType principalType, String token) {
        this.userId = userId;
        this.domainId = domainId;
        this.principalType = principalType;
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public String getDomainId() {
        return domainId;
    }

    public PrincipalType getPrincipalType() {
        return principalType;
    }

    public String getToken() {
        return token;
    }
}
