package com.adaptivebp.modules.identity.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.adaptivebp.modules.identity.model.DomainUser;
import com.adaptivebp.modules.identity.model.OwnerAccount;
import com.adaptivebp.modules.identity.repository.DomainUserRepository;
import com.adaptivebp.modules.identity.repository.OwnerAccountRepository;
import com.adaptivebp.shared.security.AdaptiveUserDetails;
import com.adaptivebp.shared.security.PrincipalType;

/**
 * ★ Identity Module Facade — the public API for other modules.
 * Other modules should only interact with identity through this service.
 */
@Service
public class AdaptivePrincipalService {

    @Autowired
    private OwnerAccountRepository ownerAccountRepository;

    @Autowired
    private DomainUserRepository domainUserRepository;

    public Optional<AdaptiveUserDetails> loadById(String principalId, PrincipalType type, String domainId) {
        if (type == PrincipalType.OWNER) {
            return ownerAccountRepository.findById(principalId)
                    .map(owner -> AdaptiveUserDetails.owner(owner.getId(), owner.getEmail(), owner.getPasswordHash()));
        }
        if (type == PrincipalType.DOMAIN_USER) {
            if (domainId == null) {
                return Optional.empty();
            }
            return domainUserRepository.findById(principalId)
                    .filter(user -> domainId.equals(user.getDomainId()))
                    .map(this::mapDomainUser);
        }
        return Optional.empty();
    }

    public AdaptiveUserDetails mapDomainUser(DomainUser user) {
        return AdaptiveUserDetails.domainUser(user.getId(), user.getDomainId(), user.getUsername(), user.getEmail(),
                user.getPasswordHash());
    }

    public AdaptiveUserDetails mapOwner(OwnerAccount account) {
        return AdaptiveUserDetails.owner(account.getId(), account.getEmail(), account.getPasswordHash());
    }
}
