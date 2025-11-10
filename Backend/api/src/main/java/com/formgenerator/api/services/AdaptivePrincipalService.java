package com.formgenerator.api.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.formgenerator.api.models.domain.DomainUser;
import com.formgenerator.api.models.owner.OwnerAccount;
import com.formgenerator.api.repository.DomainUserRepository;
import com.formgenerator.api.repository.OwnerAccountRepository;
import com.formgenerator.platform.auth.AdaptiveUserDetails;
import com.formgenerator.platform.auth.PrincipalType;

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
