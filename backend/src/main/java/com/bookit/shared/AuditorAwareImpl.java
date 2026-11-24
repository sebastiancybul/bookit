package com.bookit.shared;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class AuditorAwareImpl implements AuditorAware<String> {

    static final String ANONYMOUS = "anonymous";
    static final String SYSTEM = "system";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.of(SYSTEM);
        }
        if (authentication instanceof AnonymousAuthenticationToken || !authentication.isAuthenticated()) {
            return Optional.of(ANONYMOUS);
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return Optional.ofNullable(jwtAuthentication.getToken().getSubject())
                    .or(() -> Optional.of(ANONYMOUS));
        }
        return Optional.of(authentication.getName());
    }
}
