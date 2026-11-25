package com.bookit.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookit.TestcontainersConfiguration;
import com.bookit.auth.config.SecurityConfig;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, AvailabilityFlowIntegrationTest.FixedClockConfig.class})
class AvailabilityFlowIntegrationTest {

    private static final String THURSDAY = "2026-11-26";
    private static final String FRIDAY = "2026-11-27";
    private static final String EMPLOYEES_URL = "/api/v1/owner/business/employees";
    private static final String OFFERINGS_URL = "/api/v1/owner/business/offerings";

    private static final String WEEK_JSON = """
            {"intervals": [
              {"dayOfWeek": 4, "startTime": "09:00", "endTime": "13:00"},
              {"dayOfWeek": 5, "startTime": "09:00", "endTime": "11:00"}
            ]}
            """;

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-11-26T09:20:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    MockMvcTester mvc;

    Cookie xsrf;

    @BeforeEach
    void fetchXsrfCookie() {
        xsrf = mvc.get().uri("/api/v1/csrf").exchange().getResponse().getCookie("XSRF-TOKEN");
    }

    @Test
    void anonymousGetsSlotsFromScheduleOfferingAndBufferOfTheBusiness() throws Exception {
        Bookable setup = bookableEmployee();

        assertThat(mvc.get().uri(availabilityUrl(setup.slug(), setup.offeringId(), setup.employeeId(), FRIDAY)))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$[*].localTime").asArray()
                            .containsExactly("09:00", "09:15", "09:30", "09:45", "10:00", "10:15");
                    assertThat(json).extractingPath("$[0].startTime").isEqualTo("2026-11-27T08:00:00Z");
                });
    }

    @Test
    void todayOffersOnlyStartsAtLeastOneHourFromNow() throws Exception {
        Bookable setup = bookableEmployee();

        assertThat(mvc.get().uri(availabilityUrl(setup.slug(), setup.offeringId(), setup.employeeId(), THURSDAY)))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$[*].localTime")
                .asArray()
                .containsExactly("11:30", "11:45", "12:00", "12:15");
    }

    @Test
    void employeeWithoutScheduleHasNoSlots() throws Exception {
        Bookable setup = bookableEmployee();
        String bartekId = createEmployee(setup.owner(), "Bartek W.");
        assignOffering(setup.owner(), bartekId, setup.offeringId());

        assertThat(mvc.get().uri(availabilityUrl(setup.slug(), setup.offeringId(), bartekId, FRIDAY)))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$")
                .asArray()
                .isEmpty();
    }

    @Test
    void dateOutsideBookingWindowReturns400OnDateField() throws Exception {
        Bookable setup = bookableEmployee();

        assertThat(mvc.get().uri(availabilityUrl(setup.slug(), setup.offeringId(), setup.employeeId(), "2026-11-25")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[0].field")
                .isEqualTo("date");
        assertThat(mvc.get().uri(availabilityUrl(setup.slug(), setup.offeringId(), setup.employeeId(), "2027-05-26")))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void employeeOrOfferingThatCannotBeBookedReturns404() throws Exception {
        Bookable setup = bookableEmployee();
        String withoutOfferingId = createEmployee(setup.owner(), "Celina Z.");
        send(HttpMethod.PUT, scheduleUrl(withoutOfferingId), setup.owner(), WEEK_JSON);
        Bookable otherBusiness = bookableEmployee();

        assertThat(mvc.get().uri(availabilityUrl(setup.slug(), setup.offeringId(), withoutOfferingId, FRIDAY)))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri(availabilityUrl(setup.slug(), otherBusiness.offeringId(), setup.employeeId(), FRIDAY)))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri(availabilityUrl(setup.slug(), setup.offeringId(), otherBusiness.employeeId(), FRIDAY)))
                .hasStatus(HttpStatus.NOT_FOUND);

        send(HttpMethod.DELETE, EMPLOYEES_URL + "/" + setup.employeeId(), setup.owner(), null);
        assertThat(mvc.get().uri(availabilityUrl(setup.slug(), setup.offeringId(), setup.employeeId(), FRIDAY)))
                .hasStatus(HttpStatus.NOT_FOUND);

        send(HttpMethod.DELETE, OFFERINGS_URL + "/" + otherBusiness.offeringId(), otherBusiness.owner(), null);
        assertThat(mvc.get().uri(availabilityUrl(
                        otherBusiness.slug(), otherBusiness.offeringId(), otherBusiness.employeeId(), FRIDAY)))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    private record Bookable(Cookie owner, String slug, String offeringId, String employeeId) {
    }

    private Bookable bookableEmployee() throws Exception {
        String registerJson = """
                {"email": "%s", "password": "Testowe123!", "firstName": "Anna",
                 "lastName": "Nowak", "phone": "+48600123456"}
                """.formatted("availability-owner-" + UUID.randomUUID() + "@test.pl");
        MvcTestResult registered = send(HttpMethod.POST, "/api/v1/auth/register", null, registerJson);
        assertThat(registered).hasStatus(HttpStatus.CREATED);
        Cookie clientToken = registered.getResponse().getCookie(SecurityConfig.ACCESS_TOKEN_COOKIE);

        String businessJson = """
                {"name": "Shop", "categoryCode": "BARBER", "phone": "123456789",
                 "street": "Floriańska 1", "city": "Kraków", "postalCode": "31-019"}
                """;
        MvcTestResult business = send(HttpMethod.POST, "/api/v1/owner/business", clientToken, businessJson);
        assertThat(business).hasStatus(HttpStatus.CREATED);
        Cookie owner = business.getResponse().getCookie(SecurityConfig.ACCESS_TOKEN_COOKIE);
        String slug = JsonPath.read(business.getResponse().getContentAsString(), "$.slug");

        String offeringJson = """
                {"name": "Haircut", "priceAmount": 5000, "durationMinutes": 30, "bufferMinutes": 15}
                """;
        MvcTestResult offering = send(HttpMethod.POST, OFFERINGS_URL, owner, offeringJson);
        assertThat(offering).hasStatus(HttpStatus.CREATED);
        String offeringId = JsonPath.read(offering.getResponse().getContentAsString(), "$.id");

        String employeeId = createEmployee(owner, "Anna K.");
        assignOffering(owner, employeeId, offeringId);
        assertThat(send(HttpMethod.PUT, scheduleUrl(employeeId), owner, WEEK_JSON)).hasStatus(HttpStatus.OK);
        return new Bookable(owner, slug, offeringId, employeeId);
    }

    private String createEmployee(Cookie owner, String displayName) throws Exception {
        MvcTestResult created = send(HttpMethod.POST, EMPLOYEES_URL, owner,
                "{\"displayName\": \"%s\"}".formatted(displayName));
        assertThat(created).hasStatus(HttpStatus.CREATED);
        return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    }

    private void assignOffering(Cookie owner, String employeeId, String offeringId) {
        assertThat(send(HttpMethod.PUT, EMPLOYEES_URL + "/" + employeeId + "/offerings", owner,
                        "{\"offeringIds\": [\"%s\"]}".formatted(offeringId)))
                .hasStatus(HttpStatus.OK);
    }

    private static String scheduleUrl(String employeeId) {
        return EMPLOYEES_URL + "/" + employeeId + "/schedule";
    }

    private static String availabilityUrl(String slug, String offeringId, String employeeId, String date) {
        return "/api/v1/businesses/%s/availability?offeringId=%s&employeeId=%s&date=%s"
                .formatted(slug, offeringId, employeeId, date);
    }

    private MvcTestResult send(HttpMethod method, String uri, Cookie token, String json) {
        var request = mvc.method(method).uri(uri)
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue());
        if (token != null) {
            request = request.cookie(token);
        }
        if (json != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return request.exchange();
    }
}
