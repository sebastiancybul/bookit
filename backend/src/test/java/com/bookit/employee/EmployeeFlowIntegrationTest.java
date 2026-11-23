package com.bookit.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookit.TestcontainersConfiguration;
import com.bookit.auth.config.SecurityConfig;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EmployeeFlowIntegrationTest {

    private static final String EMPLOYEES_URL = "/api/v1/owner/business/employees";
    private static final String OFFERINGS_URL = "/api/v1/owner/business/offerings";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    Cookie xsrf;

    @BeforeEach
    void fetchXsrfCookie() {
        xsrf = mvc.get().uri("/api/v1/csrf").exchange().getResponse().getCookie("XSRF-TOKEN");
    }

    @Test
    void ownerCreatesListsUpdatesAssignsAndSoftDeletesEmployees() throws Exception {
        Cookie owner = becomeOwner();
        String haircut = createOffering(owner, "Haircut");
        String beard = createOffering(owner, "Beard trim");
        String annaId = createEmployee(owner, "Anna K.");
        createEmployee(owner, "Bartek W.");

        assertThat(send(HttpMethod.PUT, EMPLOYEES_URL + "/" + annaId, owner,
                        "{\"displayName\": \"  Anna Kowalska \", \"position\": \"Senior barber\"}"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.displayName").isEqualTo("Anna Kowalska");
                    assertThat(json).extractingPath("$.position").isEqualTo("Senior barber");
                });

        assertThat(assign(owner, annaId, haircut, beard))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.offeringIds")
                .asArray()
                .containsExactlyInAnyOrder(haircut, beard);

        assertThat(mvc.get().uri(EMPLOYEES_URL).cookie(owner))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$[*].displayName").asArray()
                            .containsExactly("Anna Kowalska", "Bartek W.");
                    assertThat(json).extractingPath("$[0].offeringIds").asArray().hasSize(2);
                    assertThat(json).extractingPath("$[1].offeringIds").asArray().isEmpty();
                });

        assertThat(send(HttpMethod.DELETE, EMPLOYEES_URL + "/" + annaId, owner, null))
                .hasStatus(HttpStatus.NO_CONTENT);

        assertThat(mvc.get().uri(EMPLOYEES_URL).cookie(owner))
                .bodyJson()
                .extractingPath("$[*].displayName")
                .asArray()
                .containsExactly("Bartek W.");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT active FROM employees WHERE id = ?::uuid", Boolean.class, annaId))
                .isFalse();
        assertThat(send(HttpMethod.DELETE, EMPLOYEES_URL + "/" + annaId, owner, null))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void assignReplacesWholeListAndEmptyListClearsIt() throws Exception {
        Cookie owner = becomeOwner();
        String haircut = createOffering(owner, "Haircut");
        String beard = createOffering(owner, "Beard trim");
        String annaId = createEmployee(owner, "Anna K.");
        assign(owner, annaId, haircut, beard);

        assertThat(assign(owner, annaId, beard))
                .bodyJson()
                .extractingPath("$.offeringIds")
                .asArray()
                .containsExactly(beard);
        assertThat(assign(owner, annaId))
                .bodyJson()
                .extractingPath("$.offeringIds")
                .asArray()
                .isEmpty();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM employee_offerings WHERE employee_id = ?::uuid", Integer.class, annaId))
                .isZero();
    }

    @Test
    void ownerCannotSeeUpdateAssignOrDeleteEmployeeOfAnotherBusiness() throws Exception {
        Cookie ownerA = becomeOwner();
        Cookie ownerB = becomeOwner();
        String employeeOfA = createEmployee(ownerA, "Anna K.");
        String offeringOfB = createOffering(ownerB, "Haircut");

        assertThat(mvc.get().uri(EMPLOYEES_URL).cookie(ownerB))
                .bodyJson()
                .extractingPath("$")
                .asArray()
                .isEmpty();
        assertThat(send(HttpMethod.PUT, EMPLOYEES_URL + "/" + employeeOfA, ownerB, "{\"displayName\": \"Hijacked\"}"))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(assign(ownerB, employeeOfA, offeringOfB))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send(HttpMethod.DELETE, EMPLOYEES_URL + "/" + employeeOfA, ownerB, null))
                .hasStatus(HttpStatus.NOT_FOUND);

        assertThat(mvc.get().uri(EMPLOYEES_URL).cookie(ownerA))
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$[0].displayName").isEqualTo("Anna K.");
                    assertThat(json).extractingPath("$[0].offeringIds").asArray().isEmpty();
                });
    }

    @Test
    void ownerCannotAssignOfferingOfAnotherBusinessToOwnEmployee() throws Exception {
        Cookie ownerA = becomeOwner();
        Cookie ownerB = becomeOwner();
        String haircutOfA = createOffering(ownerA, "Haircut");
        String offeringOfB = createOffering(ownerB, "Manicure");
        String annaId = createEmployee(ownerA, "Anna K.");
        assign(ownerA, annaId, haircutOfA);

        assertThat(assign(ownerA, annaId, haircutOfA, offeringOfB))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[0].field")
                .isEqualTo("offeringIds");

        assertThat(jdbcTemplate.queryForList(
                "SELECT offering_id::text FROM employee_offerings WHERE employee_id = ?::uuid", String.class, annaId))
                .containsExactly(haircutOfA);
    }

    @Test
    void deletedOrUnknownOfferingCannotBeAssigned() throws Exception {
        Cookie owner = becomeOwner();
        String deleted = createOffering(owner, "Old service");
        send(HttpMethod.DELETE, OFFERINGS_URL + "/" + deleted, owner, null);
        String annaId = createEmployee(owner, "Anna K.");

        assertThat(assign(owner, annaId, deleted)).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(assign(owner, annaId, UUID.randomUUID().toString())).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void duplicateDisplayNameIgnoringCaseReturns409OnCreateAndUpdate() throws Exception {
        Cookie owner = becomeOwner();
        createEmployee(owner, "Anna K.");
        String bartekId = createEmployee(owner, "Bartek W.");

        assertThat(send(HttpMethod.POST, EMPLOYEES_URL, owner, "{\"displayName\": \" anna k. \"}"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("EMPLOYEE_NAME_TAKEN");
        assertThat(send(HttpMethod.PUT, EMPLOYEES_URL + "/" + bartekId, owner, "{\"displayName\": \"ANNA K.\"}"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("EMPLOYEE_NAME_TAKEN");
    }

    @Test
    void displayNameOfDeletedEmployeeCanBeReusedAndSameNameWorksInAnotherBusiness() throws Exception {
        Cookie owner = becomeOwner();
        String oldId = createEmployee(owner, "Anna K.");
        send(HttpMethod.DELETE, EMPLOYEES_URL + "/" + oldId, owner, null);

        assertThat(send(HttpMethod.POST, EMPLOYEES_URL, owner, "{\"displayName\": \"Anna K.\"}"))
                .hasStatus(HttpStatus.CREATED);
        assertThat(send(HttpMethod.POST, EMPLOYEES_URL, becomeOwner(), "{\"displayName\": \"Anna K.\"}"))
                .hasStatus(HttpStatus.CREATED);
    }

    @Test
    void publicPageListsOnlyActiveEmployeesWithOfferingIdsAndHidesInactiveBusiness() throws Exception {
        Cookie owner = becomeOwner();
        String haircut = createOffering(owner, "Haircut");
        String annaId = createEmployee(owner, "Anna K.");
        assign(owner, annaId, haircut);
        String deletedId = createEmployee(owner, "Former employee");
        send(HttpMethod.DELETE, EMPLOYEES_URL + "/" + deletedId, owner, null);
        String slug = JsonPath.read(
                mvc.get().uri("/api/v1/owner/business").cookie(owner).exchange().getResponse().getContentAsString(),
                "$.slug");
        String publicUrl = "/api/v1/businesses/" + slug + "/employees";

        assertThat(mvc.get().uri(publicUrl))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$[*].displayName").asArray().containsExactly("Anna K.");
                    assertThat(json).extractingPath("$[0].offeringIds").asArray().containsExactly(haircut);
                });

        jdbcTemplate.update("UPDATE businesses SET active = false WHERE slug = ?", slug);

        assertThat(mvc.get().uri(publicUrl)).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void ownerListLoadsAssignmentsWithoutNPlusOneQueries() throws Exception {
        Cookie owner = becomeOwner();
        String haircut = createOffering(owner, "Haircut");
        String beard = createOffering(owner, "Beard trim");
        for (String name : new String[] {"Anna K.", "Bartek W.", "Celina Z."}) {
            assign(owner, createEmployee(owner, name), haircut, beard);
        }
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        assertThat(mvc.get().uri(EMPLOYEES_URL).cookie(owner))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$[*].offeringIds")
                .asArray()
                .hasSize(3);

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    void databaseRejectsInactiveEmployeeWithLinkedAccount() throws Exception {
        Cookie owner = becomeOwner();
        String annaId = createEmployee(owner, "Anna K.");
        String userId = JsonPath.read(
                mvc.get().uri("/api/v1/auth/me").cookie(owner).exchange().getResponse().getContentAsString(), "$.id");

        assertThatThrownBy(() -> jdbcTemplate.update(
                        "UPDATE employees SET user_id = ?::uuid, active = false WHERE id = ?::uuid", userId, annaId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_employees_inactive_unlinked");
    }

    private Cookie becomeOwner() throws Exception {
        String registerJson = """
                {"email": "%s", "password": "Testowe123!", "firstName": "Anna",
                 "lastName": "Nowak", "phone": "+48600123456"}
                """.formatted("employee-owner-" + UUID.randomUUID() + "@test.pl");
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
        String json = """
                {"name": "%s", "priceAmount": 5000, "durationMinutes": 30, "bufferMinutes": 0}
                """.formatted(name);
        MvcTestResult created = send(HttpMethod.POST, OFFERINGS_URL, owner, json);
        assertThat(created).hasStatus(HttpStatus.CREATED);
        return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    }

    private String createEmployee(Cookie owner, String displayName) throws Exception {
        MvcTestResult created = send(HttpMethod.POST, EMPLOYEES_URL, owner,
                "{\"displayName\": \"%s\"}".formatted(displayName));
        assertThat(created).hasStatus(HttpStatus.CREATED);
        return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    }

    private MvcTestResult assign(Cookie owner, String employeeId, String... offeringIds) {
        String ids = String.join("\", \"", offeringIds);
        String json = offeringIds.length == 0
                ? "{\"offeringIds\": []}"
                : "{\"offeringIds\": [\"" + ids + "\"]}";
        return send(HttpMethod.PUT, EMPLOYEES_URL + "/" + employeeId + "/offerings", owner, json);
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
}
