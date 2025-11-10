package com.formgenerator.platform.auth;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AdaptiveUserDetails implements UserDetails {

    private final String id;
    private final PrincipalType principalType;
    private final String domainId;
    private final String username;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public AdaptiveUserDetails(String id,
                               PrincipalType principalType,
                               String domainId,
                               String username,
                               String email,
                               String password,
                               Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.principalType = principalType;
        this.domainId = domainId;
        this.username = username;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }

    public static AdaptiveUserDetails owner(String id, String email, String password) {
        return new AdaptiveUserDetails(
                id,
                PrincipalType.OWNER,
                null,
                email,
                email,
                password,
                List.of(new SimpleGrantedAuthority(PrincipalType.OWNER.name())));
    }

    public static AdaptiveUserDetails domainUser(String id, String domainId, String username, String email, String password) {
        return new AdaptiveUserDetails(
                id,
                PrincipalType.DOMAIN_USER,
                domainId,
                username,
                email,
                password,
                List.of(new SimpleGrantedAuthority(PrincipalType.DOMAIN_USER.name())));
    }

    public String getId() {
        return id;
    }

    public PrincipalType getPrincipalType() {
        return principalType;
    }

    public String getDomainId() {
        return domainId;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AdaptiveUserDetails that = (AdaptiveUserDetails) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
