package com.bookit.offering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookit.TestcontainersConfiguration;
import com.bookit.auth.config.SecurityConfig;
import com.bookit.offering.entity.Offering;
import com.bookit.offering.repository.OfferingRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OfferingFlowIntegrationTest {

    private static final String OFFERINGS_URL = "/api/v1/owner/business/offerings";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    OfferingRepository offeringRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    Cookie xsrf;

    @BeforeEach
    void fetchXsrfCookie() {
        xsrf = mvc.get().uri("/api/v1/csrf").exchange().getResponse().getCookie("XSRF-TOKEN");
    }

    @Test
    void ownerCreatesListsUpdatesAndSoftDeletesOfferings() throws Exception {
        Cookie owner = becomeOwner();
        String haircutId = createOffering(owner, "Haircut");
        createOffering(owner, "Beard trim");

        assertThat(mvc.get().uri(OFFERINGS_URL).cookie(owner))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$[*].name")
                .asArray()
                .containsExactly("Beard trim", "Haircut");

        assertThat(send(HttpMethod.PUT, OFFERINGS_URL + "/" + haircutId, owner,
                        offeringJson("  Haircut deluxe  ", 9000, 45, 15)))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.name").isEqualTo("Haircut deluxe");
                    assertThat(json).extractingPath("$.priceAmount").isEqualTo(9000);
                    assertThat(json).extractingPath("$.currency").isEqualTo("PLN");
                });

        assertThat(send(HttpMethod.DELETE, OFFERINGS_URL + "/" + haircutId, owner, null))
                .hasStatus(HttpStatus.NO_CONTENT);

        assertThat(mvc.get().uri(OFFERINGS_URL).cookie(owner))
                .bodyJson()
                .extractingPath("$[*].name")
                .asArray()
                .containsExactly("Beard trim");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT active FROM offerings WHERE id = ?::uuid", Boolean.class, haircutId))
                .isFalse();
        assertThat(send(HttpMethod.DELETE, OFFERINGS_URL + "/" + haircutId, owner, null))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void auditColumnsHoldOwnerIdForOfferingAndAnonymousForRegisteredUser() throws Exception {
        Cookie owner = becomeOwner();
        String ownerId = userIdOf(owner);
        String haircutId = createOffering(owner, "Haircut");

        assertThat(jdbcTemplate.queryForMap(
                "SELECT created_by, updated_by FROM offerings WHERE id = ?::uuid", haircutId))
                .containsEntry("created_by", ownerId)
                .containsEntry("updated_by", ownerId);

        assertThat(send(HttpMethod.PUT, OFFERINGS_URL + "/" + haircutId, owner,
                        offeringJson("Haircut deluxe", 9000, 45, 15)))
                .hasStatus(HttpStatus.OK);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT updated_by FROM offerings WHERE id = ?::uuid", String.class, haircutId))
                .isEqualTo(ownerId);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT created_by FROM users WHERE id = ?::uuid", String.class, ownerId))
                .isEqualTo("anonymous");
    }

    @Test
    void ownerCannotSeeUpdateOrDeleteOfferingOfAnotherBusiness() throws Exception {
        Cookie ownerA = becomeOwner();
        Cookie ownerB = becomeOwner();
        String offeringOfA = createOffering(ownerA, "Manicure");

        assertThat(mvc.get().uri(OFFERINGS_URL).cookie(ownerB))
                .bodyJson()
                .extractingPath("$")
                .asArray()
                .isEmpty();
        assertThat(send(HttpMethod.PUT, OFFERINGS_URL + "/" + offeringOfA, ownerB,
                        offeringJson("Hijacked", 0, 30, 0)))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send(HttpMethod.DELETE, OFFERINGS_URL + "/" + offeringOfA, ownerB, null))
                .hasStatus(HttpStatus.NOT_FOUND);

        assertThat(mvc.get().uri(OFFERINGS_URL).cookie(ownerA))
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$[0].name").isEqualTo("Manicure");
                    assertThat(json).extractingPath("$[0].priceAmount").isEqualTo(5000);
                });
    }

    @Test
    void duplicateNameIgnoringCaseAndSurroundingSpacesReturns409OnCreateAndUpdate() throws Exception {
        Cookie owner = becomeOwner();
        createOffering(owner, "Haircut");
        String beardId = createOffering(owner, "Beard trim");

        assertThat(send(HttpMethod.POST, OFFERINGS_URL, owner, offeringJson("  HAIRCUT ", 5000, 30, 0)))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("OFFERING_NAME_TAKEN");
        assertThat(send(HttpMethod.PUT, OFFERINGS_URL + "/" + beardId, owner, offeringJson("haircut", 5000, 30, 0)))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("OFFERING_NAME_TAKEN");

        assertThat(mvc.get().uri(OFFERINGS_URL).cookie(owner))
                .bodyJson()
                .extractingPath("$[*].name")
                .asArray()
                .containsExactly("Beard trim", "Haircut");
    }

    @Test
    void nameOfDeletedOfferingCanBeReused() throws Exception {
        Cookie owner = becomeOwner();
        String oldId = createOffering(owner, "Haircut");
        send(HttpMethod.DELETE, OFFERINGS_URL + "/" + oldId, owner, null);

        assertThat(send(HttpMethod.POST, OFFERINGS_URL, owner, offeringJson("Haircut", 6000, 30, 0)))
                .hasStatus(HttpStatus.CREATED);
    }

    @Test
    void sameNameIsAllowedInDifferentBusinesses() throws Exception {
        createOffering(becomeOwner(), "Haircut");

        assertThat(send(HttpMethod.POST, OFFERINGS_URL, becomeOwner(), offeringJson("Haircut", 5000, 30, 0)))
                .hasStatus(HttpStatus.CREATED);
    }

    @Test
    void publicPageListsOnlyActiveOfferingsWithoutBufferAndHidesInactiveBusiness() throws Exception {
        Cookie owner = becomeOwner();
        createOffering(owner, "Haircut");
        String deletedId = createOffering(owner, "Old service");
        send(HttpMethod.DELETE, OFFERINGS_URL + "/" + deletedId, owner, null);
        String slug = JsonPath.read(
                mvc.get().uri("/api/v1/owner/business").cookie(owner).exchange().getResponse().getContentAsString(),
                "$.slug");
        String publicUrl = "/api/v1/businesses/" + slug + "/offerings";

        assertThat(mvc.get().uri(publicUrl))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$[*].name").asArray().containsExactly("Haircut");
                    assertThat(json).doesNotHavePath("$[0].bufferMinutes");
                });

        jdbcTemplate.update("UPDATE businesses SET active = false WHERE slug = ?", slug);

        assertThat(mvc.get().uri(publicUrl)).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void databaseRejectsDurationThatIsNotMultipleOfFive() throws Exception {
        UUID businessId = UUID.fromString(businessIdOf(becomeOwner()));

        assertThatThrownBy(() -> offeringRepository.saveAndFlush(new Offering(businessId, "Odd", null, 5000, 37, 0)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .cause()
                .isInstanceOf(ConstraintViolationException.class)
                .extracting("constraintName")
                .isEqualTo("ck_offerings_duration");
    }

    private String businessIdOf(Cookie owner) throws Exception {
        return jdbcTemplate.queryForObject(
                "SELECT id::text FROM businesses WHERE owner_id = ?::uuid", String.class, userIdOf(owner));
    }

    private String userIdOf(Cookie user) throws Exception {
        return JsonPath.read(
                mvc.get().uri("/api/v1/auth/me").cookie(user).exchange().getResponse().getContentAsString(), "$.id");
    }

    private Cookie becomeOwner() throws Exception {
        String registerJson = """
                {"email": "%s", "password": "Testowe123!", "firstName": "Anna",
                 "lastName": "Nowak", "phone": "+48600123456"}
                """.formatted("offering-owner-" + UUID.randomUUID() + "@test.pl");
        MvcTestResult registered = mvc.post().uri("/api/v1/auth/register")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson)
                .exchange();
        assertThat(registered).hasStatus(HttpStatus.CREATED);
        Cookie clientToken = registered.getResponse().getCookie(SecurityConfig.ACCESS_TOKEN_COOKIE);

        String businessJson = """
                {"name": "Shop", "categoryCode": "BARBER", "phone": "123456789",
                 "street": "Floriańska 1", "city": "Kraków", "postalCode": "31-019"}
                """;
        MvcTestResult created = send(HttpMethod.POST, "/api/v1/owner/business", clientToken, businessJson);
        assertThat(created).hasStatus(HttpStatus.CREATED);
        return created.getResponse().getCookie(SecurityConfig.ACCESS_TOKEN_COOKIE);
    }

    private String createOffering(Cookie owner, String name) throws Exception {
        MvcTestResult created = send(HttpMethod.POST, OFFERINGS_URL, owner, offeringJson(name, 5000, 30, 10));
        assertThat(created).hasStatus(HttpStatus.CREATED);
        return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    }

    private MvcTestResult send(HttpMethod method, String uri, Cookie token, String json) {
        var request = mvc.method(method).uri(uri)
                .cookie(xsrf, token)
                .header("X-XSRF-TOKEN", xsrf.getValue());
        if (json != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return request.exchange();
    }

    private static String offeringJson(String name, long priceAmount, int durationMinutes, int bufferMinutes) {
        return """
                {"name": "%s", "priceAmount": %d, "durationMinutes": %d, "bufferMinutes": %d}
                """.formatted(name, priceAmount, durationMinutes, bufferMinutes);
    }
}
