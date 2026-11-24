package com.bookit.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AuditorAwareImplTest {

    private static final List<GrantedAuthority> CLIENT = AuthorityUtils.createAuthorityList("ROLE_CLIENT");

    private final AuditorAwareImpl auditorAware = new AuditorAwareImpl();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsUserIdFromJwtSubject() {
        String userId = UUID.randomUUID().toString();
        authenticate(new JwtAuthenticationToken(jwt().subject(userId).build(), CLIENT));

        assertThat(auditorAware.getCurrentAuditor()).contains(userId);
    }

    @Test
    void returnsAnonymousForJwtWithoutSubject() {
        authenticate(new JwtAuthenticationToken(jwt().claim("roles", "ROLE_CLIENT").build(), CLIENT));

        assertThat(auditorAware.getCurrentAuditor()).contains(AuditorAwareImpl.ANONYMOUS);
    }

    @Test
    void returnsAnonymousForUnauthenticatedJwt() {
        authenticate(new JwtAuthenticationToken(jwt().subject(UUID.randomUUID().toString()).build()));

        assertThat(auditorAware.getCurrentAuditor()).contains(AuditorAwareImpl.ANONYMOUS);
    }

    @Test
    void returnsAnonymousForUnauthenticatedRequest() {
        authenticate(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(auditorAware.getCurrentAuditor()).contains(AuditorAwareImpl.ANONYMOUS);
    }

    @Test
    void returnsNameForOtherAuthenticatedUser() {
        authenticate(UsernamePasswordAuthenticationToken.authenticated("anna@test.pl", null, CLIENT));

        assertThat(auditorAware.getCurrentAuditor()).contains("anna@test.pl");
    }

    @Test
    void returnsSystemWithoutSecurityContext() {
        assertThat(auditorAware.getCurrentAuditor()).contains(AuditorAwareImpl.SYSTEM);
    }

    private static Jwt.Builder jwt() {
        return Jwt.withTokenValue("token").header("alg", "HS256");
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
