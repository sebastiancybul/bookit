package com.bookit.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.bookit.auth.config.JwtConfig;
import com.bookit.auth.config.SecurityConfig;
import com.bookit.auth.security.AuthCookies;
import com.bookit.auth.security.ProblemDetailSecurityHandler;
import com.bookit.auth.security.TestTokens;
import com.bookit.auth.service.AuthService;
import com.bookit.shared.error.BusinessRuleException;
import com.bookit.user.dto.UserResponse;
import jakarta.servlet.http.Cookie;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(controllers = {AuthController.class, CsrfController.class})
@Import({SecurityConfig.class, JwtConfig.class, AuthCookies.class, ProblemDetailSecurityHandler.class})
class AuthControllerTest {

    private static final String LOGIN_JSON = """
            {"email": "jan@test.pl", "password": "Test1234!"}
            """;

    private static final String REGISTER_JSON = """
            {"email": "jan@test.pl", "password": "Testowe123!", "firstName": "Jan",
             "lastName": "Testowy", "phone": "+48600123456"}
            """;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UserResponse USER =
            new UserResponse(USER_ID, "Jan", "Testowy", "jan@test.pl", Set.of("ROLE_CLIENT"));

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JwtEncoder jwtEncoder;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    UserDetailsService userDetailsService;

    @Test
    void csrfEndpointSetsXsrfCookie() {
        assertThat(mvc.get().uri("/api/v1/csrf"))
                .hasStatus(HttpStatus.NO_CONTENT)
                .cookies()
                .containsCookie("XSRF-TOKEN");
    }

    @Test
    void registerReturns201WithUserAndAccessTokenCookie() {
        given(authService.register(any())).willReturn(new AuthService.LoginResult(USER, "signed.jwt.value"));
        Cookie xsrf = fetchXsrfCookie();

        MvcTestResult result = mvc.post().uri("/api/v1/auth/register")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTER_JSON)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.email")
                .isEqualTo("jan@test.pl");
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .startsWith("access_token=signed.jwt.value")
                .contains("HttpOnly");
    }

    @Test
    void registerWithWeakPasswordReturns400WithPasswordErrors() {
        Cookie xsrf = fetchXsrfCookie();

        assertThat(mvc.post().uri("/api/v1/auth/register")
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_JSON.replace("Testowe123!", "weakpassword")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[*].field")
                .asArray()
                .containsExactly("password");
    }

    @Test
    void registerWithTakenEmailReturns409WithCode() {
        given(authService.register(any()))
                .willThrow(new BusinessRuleException("EMAIL_TAKEN", "An account with this email already exists"));
        Cookie xsrf = fetchXsrfCookie();

        assertThat(mvc.post().uri("/api/v1/auth/register")
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_JSON))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("EMAIL_TAKEN");
    }

    @Test
    void loginReturnsUserAndSetsHttpOnlyAccessTokenCookie() {
        given(authService.login(any())).willReturn(new AuthService.LoginResult(USER, "signed.jwt.value"));
        Cookie xsrf = fetchXsrfCookie();

        MvcTestResult result = mvc.post().uri("/api/v1/auth/login")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(LOGIN_JSON)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.email")
                .isEqualTo("jan@test.pl");
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .startsWith("access_token=signed.jwt.value")
                .contains("HttpOnly", "SameSite=Lax", "Path=/api", "Max-Age=28800");
    }

    @Test
    void loginWithoutCsrfTokenIsForbidden() {
        assertThat(mvc.post().uri("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void loginWithBadCredentialsReturns401WithGenericMessage() {
        given(authService.login(any())).willThrow(new BadCredentialsException("Bad credentials"));
        Cookie xsrf = fetchXsrfCookie();

        assertThat(mvc.post().uri("/api/v1/auth/login")
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Invalid email or password");
    }

    @Test
    void loginWithInvalidEmailReturns400WithFieldError() {
        Cookie xsrf = fetchXsrfCookie();

        assertThat(mvc.post().uri("/api/v1/auth/login")
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "not-an-email", "password": "Test1234!"}
                                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[0].field")
                .isEqualTo("email");
    }

    @Test
    void meWithoutTokenReturns401() {
        assertThat(mvc.get().uri("/api/v1/auth/me")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void meWithValidTokenCookieReturnsCurrentUser() {
        given(authService.me(USER_ID)).willReturn(USER);

        assertThat(mvc.get().uri("/api/v1/auth/me").cookie(accessTokenCookie()))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.id")
                .isEqualTo(USER_ID.toString());
    }

    @Test
    void meWithTamperedTokenReturns401() {
        Cookie tampered = new Cookie(SecurityConfig.ACCESS_TOKEN_COOKIE, accessTokenCookie().getValue() + "x");

        assertThat(mvc.get().uri("/api/v1/auth/me").cookie(tampered)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void authenticatedRequestDoesNotRotateCsrfToken() {
        given(authService.me(USER_ID)).willReturn(USER);
        Cookie xsrf = fetchXsrfCookie();

        MvcTestResult result = mvc.get().uri("/api/v1/auth/me")
                .cookie(xsrf, accessTokenCookie())
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .noneMatch(header -> header.startsWith("XSRF-TOKEN"));
    }

    @Test
    void logoutClearsAccessTokenCookie() {
        Cookie xsrf = fetchXsrfCookie();

        MvcTestResult result = mvc.post().uri("/api/v1/auth/logout")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .exchange();

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .startsWith("access_token=;")
                .contains("Max-Age=0", "Path=/api");
    }

    private Cookie fetchXsrfCookie() {
        return mvc.get().uri("/api/v1/csrf").exchange().getResponse().getCookie("XSRF-TOKEN");
    }

    private Cookie accessTokenCookie() {
        return TestTokens.accessTokenCookie(jwtEncoder, USER_ID, "ROLE_CLIENT");
    }
}
