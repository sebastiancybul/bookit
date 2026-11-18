package com.bookit.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookit.auth.controller.CsrfController;
import com.bookit.auth.security.ProblemDetailSecurityHandler;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(controllers = CsrfController.class)
@Import({SecurityConfig.class, JwtConfig.class, ProblemDetailSecurityHandler.class})
class SecurityConfigTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JwtEncoder jwtEncoder;

    @MockitoBean
    UserDetailsService userDetailsService;

    @Test
    void anonymousRequestToUnknownPathReturns401ProblemDetail() {
        MvcTestResult result = mvc.get().uri("/api/v1/unknown").exchange();

        assertProblem(result, HttpStatus.UNAUTHORIZED, "Authentication required");
        assertThat(result).bodyJson().extractingPath("$.instance").isEqualTo("/api/v1/unknown");
    }

    @Test
    void authenticatedRequestToUnknownPathIsDeniedByDefault() {
        MvcTestResult result = mvc.get().uri("/api/v1/unknown").cookie(tokenCookie("ROLE_CLIENT")).exchange();

        assertProblem(result, HttpStatus.FORBIDDEN, "Access denied");
    }

    @Test
    void protectedEndpointWithoutTokenReturns401ProblemDetail() {
        MvcTestResult result = mvc.get().uri("/api/v1/auth/me").exchange();

        assertProblem(result, HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    @Test
    void tamperedTokenReturns401ProblemDetail() {
        Cookie tampered = new Cookie(SecurityConfig.ACCESS_TOKEN_COOKIE, tokenCookie("ROLE_CLIENT").getValue() + "x");

        MvcTestResult result = mvc.get().uri("/api/v1/auth/me").cookie(tampered).exchange();

        assertProblem(result, HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    @Test
    void postWithoutCsrfTokenReturns403ProblemDetail() {
        MvcTestResult result = mvc.post().uri("/api/v1/auth/logout").exchange();

        assertProblem(result, HttpStatus.FORBIDDEN, "Access denied");
    }

    private void assertProblem(MvcTestResult result, HttpStatus status, String detail) {
        assertThat(result)
                .hasStatus(status)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo(detail);
    }

    private Cookie tokenCookie(String role) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plusSeconds(600))
                .claim("roles", List.of(role))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new Cookie(SecurityConfig.ACCESS_TOKEN_COOKIE, token);
    }
}
