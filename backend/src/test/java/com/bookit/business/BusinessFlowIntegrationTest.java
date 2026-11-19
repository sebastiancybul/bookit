package com.bookit.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookit.TestcontainersConfiguration;
import com.bookit.auth.config.SecurityConfig;
import com.bookit.business.entity.Business;
import com.bookit.business.entity.BusinessCategory;
import com.bookit.business.repository.BusinessCategoryRepository;
import com.bookit.business.repository.BusinessRepository;
import com.bookit.user.entity.Role;
import com.bookit.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BusinessFlowIntegrationTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    BusinessRepository businessRepository;

    @Autowired
    BusinessCategoryRepository categoryRepository;

    @Test
    void clientCreatesBusinessAndBecomesOwnerWithoutLoggingInAgain() throws Exception {
        Cookie xsrf = fetchXsrfCookie();
        RegisteredUser user = register(xsrf);

        assertThat(mvc.get().uri("/api/v1/owner/business").cookie(user.token())).hasStatus(HttpStatus.FORBIDDEN);

        MvcTestResult created = send(HttpMethod.POST, xsrf, user.token(), businessJson("Barber Shop", "BARBER"));
        assertThat(created).hasStatus(HttpStatus.CREATED);
        String publicId = JsonPath.read(created.getResponse().getContentAsString(), "$.publicId");
        Cookie ownerToken = created.getResponse().getCookie(SecurityConfig.ACCESS_TOKEN_COOKIE);
        assertThat(ownerToken).isNotNull();

        assertThat(mvc.get().uri("/api/v1/auth/me").cookie(ownerToken))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.roles")
                .asArray()
                .containsExactlyInAnyOrder("ROLE_CLIENT", "ROLE_OWNER");
        assertThat(mvc.get().uri("/api/v1/owner/business").cookie(ownerToken))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.publicId")
                .isEqualTo(publicId);

        assertThat(send(HttpMethod.POST, xsrf, ownerToken, businessJson("Second Shop", "BARBER")))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("BUSINESS_ALREADY_EXISTS");
    }

    @Test
    void ownerSeesAndUpdatesOnlyOwnBusiness() throws Exception {
        Cookie xsrf = fetchXsrfCookie();
        Cookie ownerA = becomeOwner(xsrf, "Shop A");
        Cookie ownerB = becomeOwner(xsrf, "Shop B");

        assertThat(mvc.get().uri("/api/v1/owner/business").cookie(ownerA))
                .bodyJson()
                .extractingPath("$.name")
                .isEqualTo("Shop A");

        String foreignOwnerId = JsonPath.read(
                mvc.get().uri("/api/v1/auth/me").cookie(ownerB).exchange().getResponse().getContentAsString(), "$.id");
        String updateWithForeignOwner = businessJson("Shop A Renamed", "BEAUTY")
                .replace("{", "{\"ownerId\": \"" + foreignOwnerId + "\", ");

        assertThat(send(HttpMethod.PUT, xsrf, ownerA, updateWithForeignOwner))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.name")
                .isEqualTo("Shop A Renamed");
        assertThat(mvc.get().uri("/api/v1/owner/business").cookie(ownerB))
                .bodyJson()
                .extractingPath("$.name")
                .isEqualTo("Shop B");
    }

    @Test
    void unknownCategoryReturns400AndGrantsNothing() throws Exception {
        Cookie xsrf = fetchXsrfCookie();
        RegisteredUser user = register(xsrf);

        assertThat(send(HttpMethod.POST, xsrf, user.token(), businessJson("Florist", "FLORIST")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[0].field")
                .isEqualTo("categoryCode");

        assertThat(businessRepository.existsByOwnerId(user.id())).isFalse();
        assertThat(userRepository.findById(user.id()).orElseThrow().getRoles())
                .extracting(Role::getName)
                .containsExactly("ROLE_CLIENT");
    }

    @Test
    void secondBusinessOfSameOwnerViolatesNamedOwnerConstraint() throws Exception {
        RegisteredUser user = register(fetchXsrfCookie());
        BusinessCategory barber = categoryRepository.findByCode("BARBER").orElseThrow();
        businessRepository.saveAndFlush(newBusiness(user.id(), "aaaaaaaa", barber));

        assertThatThrownBy(() -> businessRepository.saveAndFlush(newBusiness(user.id(), "bbbbbbbb", barber)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .cause()
                .isInstanceOf(ConstraintViolationException.class)
                .extracting("constraintName")
                .isEqualTo("uk_businesses_owner");
    }

    private static Business newBusiness(UUID ownerId, String publicId, BusinessCategory category) {
        return new Business(ownerId, publicId, "Shop", null, category, "123456789", null,
                "Floriańska 1", "Kraków", "31-019");
    }

    private Cookie becomeOwner(Cookie xsrf, String businessName) throws Exception {
        RegisteredUser user = register(xsrf);
        MvcTestResult created = send(HttpMethod.POST, xsrf, user.token(), businessJson(businessName, "BARBER"));
        assertThat(created).hasStatus(HttpStatus.CREATED);
        return created.getResponse().getCookie(SecurityConfig.ACCESS_TOKEN_COOKIE);
    }

    private RegisteredUser register(Cookie xsrf) throws Exception {
        String json = """
                {"email": "%s", "password": "Testowe123!", "firstName": "Anna",
                 "lastName": "Nowak", "phone": "+48600123456"}
                """.formatted("owner-" + UUID.randomUUID() + "@test.pl");
        MvcTestResult registered = mvc.post().uri("/api/v1/auth/register")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
        assertThat(registered).hasStatus(HttpStatus.CREATED);
        String id = JsonPath.read(registered.getResponse().getContentAsString(), "$.id");
        return new RegisteredUser(
                UUID.fromString(id), registered.getResponse().getCookie(SecurityConfig.ACCESS_TOKEN_COOKIE));
    }

    private MvcTestResult send(HttpMethod method, Cookie xsrf, Cookie token, String json) {
        return mvc.method(method).uri("/api/v1/owner/business")
                .cookie(xsrf, token)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private Cookie fetchXsrfCookie() {
        return mvc.get().uri("/api/v1/csrf").exchange().getResponse().getCookie("XSRF-TOKEN");
    }

    private static String businessJson(String name, String categoryCode) {
        return """
                {"name": "%s", "categoryCode": "%s", "phone": "123456789",
                 "street": "Floriańska 1", "city": "Kraków", "postalCode": "31-019"}
                """.formatted(name, categoryCode);
    }

    private record RegisteredUser(UUID id, Cookie token) {
    }
}
