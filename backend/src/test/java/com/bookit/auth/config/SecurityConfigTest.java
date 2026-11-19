package com.bookit.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookit.auth.controller.CsrfController;
import com.bookit.auth.security.ProblemDetailSecurityHandler;
import com.bookit.auth.security.TestTokens;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtEncoder;
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

    @Test
    void ownerPassesOwnerPrefix() {
        MvcTestResult result = mvc.get().uri("/api/v1/owner/business")
                .cookie(tokenCookie("ROLE_CLIENT", "ROLE_OWNER"))
                .exchange();

        assertPassedSecurity(result);
    }

    @Test
    void clientIsDeniedOwnerPrefix() {
        MvcTestResult result = mvc.get().uri("/api/v1/owner/business")
                .cookie(tokenCookie("ROLE_CLIENT"))
                .exchange();

        assertProblem(result, HttpStatus.FORBIDDEN, "Access denied");
    }

    @Test
    void clientIsDeniedOwnerBusinessUpdate() {
        Cookie xsrf = fetchXsrfCookie();

        MvcTestResult result = mvc.put().uri("/api/v1/owner/business")
                .cookie(xsrf, tokenCookie("ROLE_CLIENT"))
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .exchange();

        assertProblem(result, HttpStatus.FORBIDDEN, "Access denied");
    }

    @Test
    void clientCanReachBusinessCreation() {
        Cookie xsrf = fetchXsrfCookie();

        MvcTestResult result = mvc.post().uri("/api/v1/owner/business")
                .cookie(xsrf, tokenCookie("ROLE_CLIENT"))
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .exchange();

        assertPassedSecurity(result);
    }

    @Test
    void anonymousBusinessCreationReturns401() {
        Cookie xsrf = fetchXsrfCookie();

        MvcTestResult result = mvc.post().uri("/api/v1/owner/business")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .exchange();

        assertProblem(result, HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    @Test
    void employeePassesEmployeePrefix() {
        MvcTestResult result = mvc.get().uri("/api/v1/employee/appointments")
                .cookie(tokenCookie("ROLE_CLIENT", "ROLE_EMPLOYEE"))
                .exchange();

        assertPassedSecurity(result);
    }

    @Test
    void ownerIsDeniedEmployeePrefix() {
        MvcTestResult result = mvc.get().uri("/api/v1/employee/appointments")
                .cookie(tokenCookie("ROLE_CLIENT", "ROLE_OWNER"))
                .exchange();

        assertProblem(result, HttpStatus.FORBIDDEN, "Access denied");
    }

    private void assertPassedSecurity(MvcTestResult result) {
        assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
    }

    private void assertProblem(MvcTestResult result, HttpStatus status, String detail) {
        assertThat(result)
                .hasStatus(status)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo(detail);
    }

    private Cookie tokenCookie(String... roles) {
        return TestTokens.accessTokenCookie(jwtEncoder, UUID.randomUUID(), roles);
    }

    private Cookie fetchXsrfCookie() {
        return mvc.get().uri("/api/v1/csrf").exchange().getResponse().getCookie("XSRF-TOKEN");
    }
}
