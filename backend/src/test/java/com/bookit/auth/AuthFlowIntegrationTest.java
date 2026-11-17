package com.bookit.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookit.TestcontainersConfiguration;
import com.bookit.auth.config.SecurityConfig;
import com.bookit.user.entity.Role;
import com.bookit.user.entity.User;
import com.bookit.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthFlowIntegrationTest {

    private static final String PASSWORD = "Testowe123!";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    UserRepository userRepository;

    @Test
    void registeredUserIsLoggedInAndCanLogInAgain() {
        String email = uniqueEmail();
        Cookie xsrf = fetchXsrfCookie();

        MvcTestResult registered = post("/api/v1/auth/register", xsrf, registerJson(email.toUpperCase()));
        assertThat(registered).hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.email")
                .isEqualTo(email);

        Cookie accessToken = registered.getResponse().getCookie(SecurityConfig.ACCESS_TOKEN_COOKIE);
        assertThat(accessToken).isNotNull();
        assertThat(mvc.get().uri("/api/v1/auth/me").cookie(accessToken))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.roles[0]")
                .isEqualTo("ROLE_CLIENT");

        User saved = userRepository.findByEmail(email).orElseThrow();
        assertThat(saved.getPasswordHash()).startsWith("{bcrypt}").doesNotContain(PASSWORD);
        assertThat(saved.getRoles()).extracting(Role::getName).containsExactly("ROLE_CLIENT");

        assertThat(post("/api/v1/auth/login", xsrf, loginJson(email, PASSWORD)))
                .hasStatus(HttpStatus.OK);
        assertThat(post("/api/v1/auth/login", xsrf, loginJson(email, "Wrong123!pass")))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void registeringTakenEmailInDifferentCaseReturns409() {
        String email = uniqueEmail();
        Cookie xsrf = fetchXsrfCookie();

        assertThat(post("/api/v1/auth/register", xsrf, registerJson(email)))
                .hasStatus(HttpStatus.CREATED);
        assertThat(post("/api/v1/auth/register", xsrf, registerJson(email.toUpperCase())))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("EMAIL_TAKEN");
    }

    private MvcTestResult post(String uri, Cookie xsrf, String json) {
        return mvc.post().uri(uri)
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private Cookie fetchXsrfCookie() {
        return mvc.get().uri("/api/v1/csrf").exchange().getResponse().getCookie("XSRF-TOKEN");
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.pl";
    }

    private static String registerJson(String email) {
        return """
                {"email": "%s", "password": "%s", "firstName": "Anna",
                 "lastName": "Nowak", "phone": "+48600123456"}
                """.formatted(email, PASSWORD);
    }

    private static String loginJson(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }
}
