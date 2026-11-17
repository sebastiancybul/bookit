package com.bookit.auth.security;

import com.bookit.auth.config.JwtProperties;
import com.bookit.auth.config.SecurityConfig;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthCookies {

    private final JwtProperties jwtProperties;

    public ResponseCookie accessToken(String token) {
        return build(token, jwtProperties.expiration());
    }

    public ResponseCookie clearAccessToken() {
        return build("", Duration.ZERO);
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(SecurityConfig.ACCESS_TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(jwtProperties.cookieSecure())
                .sameSite("Lax")
                .path("/api")
                .maxAge(maxAge)
                .build();
    }
}
