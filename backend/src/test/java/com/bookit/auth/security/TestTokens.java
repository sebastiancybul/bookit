package com.bookit.auth.security;

import com.bookit.auth.config.SecurityConfig;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

public final class TestTokens {

    private TestTokens() {
    }

    public static Cookie accessTokenCookie(JwtEncoder jwtEncoder, UUID userId, String... roles) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .expiresAt(Instant.now().plusSeconds(600))
                .claim("roles", List.of(roles))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new Cookie(SecurityConfig.ACCESS_TOKEN_COOKIE, token);
    }
}
