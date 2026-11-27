package com.bookit.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.bookit.TestcontainersConfiguration;
import com.bookit.appointment.entity.Appointment;
import com.bookit.appointment.repository.AppointmentRepository;
import com.bookit.auth.config.SecurityConfig;
import com.bookit.offering.service.OfferingService;
import com.bookit.offering.service.OfferingService.BookableOffering;
import com.bookit.shared.error.PgErrors;
import com.bookit.shared.error.PgErrors.Violation;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, AvailabilityFlowIntegrationTest.FixedClockConfig.class})
class BookingFlowIntegrationTest {

    private static final String FRIDAY = "2026-11-27";
    private static final String NINE_AM = "2026-11-27T08:00:00Z";
    private static final String TEN_AM = "2026-11-27T09:00:00Z";
    private static final String TEN_FIFTEEN_AM = "2026-11-27T09:15:00Z";
    private static final String APPOINTMENTS_URL = "/api/v1/appointments";
    private static final String EMPLOYEES_URL = "/api/v1/owner/business/employees";
    private static final String OFFERINGS_URL = "/api/v1/owner/business/offerings";
    private static final int PARALLEL_REQUESTS = 5;

    private static final String WEEK_JSON = """
            {"intervals": [{"dayOfWeek": 5, "startTime": "09:00", "endTime": "11:00"}]}
            """;

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AppointmentRepository appointmentRepository;

    @Autowired
    OfferingService offeringService;

    Cookie xsrf;

    @BeforeEach
    void fetchXsrfCookie() {
        xsrf = mvc.get().uri("/api/v1/csrf").exchange().getResponse().getCookie("XSRF-TOKEN");
    }

    @Test
    void bookingReturns201WithSnapshotAndHidesSlotsOverlappingAppointmentWithBuffer() throws Exception {
        Business setup = business();
        Cookie client = registerClient();

        assertThat(book(client, UUID.randomUUID(), setup, setup.employeeId(), NINE_AM, "  Short on the sides "))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.startTime").isEqualTo(NINE_AM);
                    assertThat(json).extractingPath("$.endTime").isEqualTo("2026-11-27T08:30:00Z");
                    assertThat(json).extractingPath("$.status").isEqualTo("CONFIRMED");
                    assertThat(json).extractingPath("$.offeringName").isEqualTo("Haircut");
                    assertThat(json).extractingPath("$.priceAmount").isEqualTo(5000);
                    assertThat(json).extractingPath("$.clientNote").isEqualTo("Short on the sides");
                });

        assertThat(mvc.get().uri(availabilityUrl(setup, setup.employeeId())))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$[*].localTime")
                .asArray()
                .containsExactly("09:45", "10:00", "10:15");
    }

    @Test
    void sequentialReplayWithSameKeyReturns200WithTheSameAppointment() throws Exception {
        Business setup = business();
        Cookie client = registerClient();
        UUID key = UUID.randomUUID();

        MvcTestResult first = book(client, key, setup, setup.employeeId(), NINE_AM, null);
        MvcTestResult second = book(client, key, setup, setup.employeeId(), NINE_AM, null);

        assertThat(first).hasStatus(HttpStatus.CREATED);
        assertThat(second).hasStatus(HttpStatus.OK);
        assertThat(idOf(second)).isEqualTo(idOf(first));
        assertThat(appointmentsOf(setup.employeeId())).hasSize(1);
    }

    @Test
    void sameKeyWithDifferentPayloadReturns422() throws Exception {
        Business setup = business();
        Cookie client = registerClient();
        UUID key = UUID.randomUUID();
        assertThat(book(client, key, setup, setup.employeeId(), NINE_AM, null)).hasStatus(HttpStatus.CREATED);

        assertThat(book(client, key, setup, setup.employeeId(), TEN_AM, null))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("IDEMPOTENCY_KEY_REUSED");
        assertThat(appointmentsOf(setup.employeeId())).hasSize(1);
    }

    @Test
    void concurrentRequestsWithSameKeyCreateOneAppointmentAndAllReturnIt() throws Exception {
        Business setup = business();
        Cookie client = registerClient();
        UUID key = UUID.randomUUID();

        List<Response> responses = inParallel(() -> book(client, key, setup, setup.employeeId(), NINE_AM, null));

        assertThat(responses).extracting(Response::status).containsExactlyInAnyOrder(201, 200, 200, 200, 200);
        assertThat(responses).extracting(r -> JsonPath.<String>read(r.body(), "$.id")).containsOnly(
                appointmentsOf(setup.employeeId()).getFirst().getId().toString());
        assertThat(appointmentsOf(setup.employeeId())).hasSize(1);
    }

    @Test
    void concurrentRequestsOfDifferentClientsForSameSlotBookItOnce() throws Exception {
        Business setup = business();
        List<Cookie> clients = new ArrayList<>();
        for (int i = 0; i < PARALLEL_REQUESTS; i++) {
            clients.add(registerClient());
        }
        List<Callable<MvcTestResult>> requests = clients.stream()
                .<Callable<MvcTestResult>>map(c -> () -> book(c, UUID.randomUUID(), setup, setup.employeeId(), NINE_AM, null))
                .toList();

        List<Response> responses = inParallel(requests);

        assertThat(responses).filteredOn(r -> r.status() == 201).hasSize(1);
        assertThat(responses).filteredOn(r -> r.status() == 409).hasSize(PARALLEL_REQUESTS - 1)
                .allSatisfy(r -> assertThat(JsonPath.<String>read(r.body(), "$.code")).isEqualTo("SLOT_TAKEN"));
        assertThat(appointmentsOf(setup.employeeId())).hasSize(1);
    }

    @Test
    void clientCannotBookOverlappingAppointmentsWithDifferentEmployees() throws Exception {
        Business setup = business();
        String otherEmployeeId = bookableEmployee(setup, "Bartek W.");
        Cookie client = registerClient();
        assertThat(book(client, UUID.randomUUID(), setup, setup.employeeId(), NINE_AM, null))
                .hasStatus(HttpStatus.CREATED);

        assertThat(book(client, UUID.randomUUID(), setup, otherEmployeeId, NINE_AM, null))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("CLIENT_OVERLAP");
        assertThat(appointmentsOf(UUID.fromString(otherEmployeeId))).isEmpty();
    }

    @Test
    void concurrentOverlappingBookingsOfOneClientNeverReturn500() throws Exception {
        Business setup = business();
        for (int round = 0; round < 10; round++) {
            String firstEmployeeId = bookableEmployee(setup, "Round " + round + " A");
            String secondEmployeeId = bookableEmployee(setup, "Round " + round + " B");
            Cookie client = registerClient();

            List<Response> responses = inParallel(List.of(
                    () -> book(client, UUID.randomUUID(), setup, firstEmployeeId, NINE_AM, null),
                    () -> book(client, UUID.randomUUID(), setup, secondEmployeeId, NINE_AM, null)));

            assertThat(responses).extracting(Response::status).containsExactlyInAnyOrder(201, 409);
            assertThat(responses).filteredOn(r -> r.status() == 409)
                    .singleElement()
                    .satisfies(r -> assertThat(JsonPath.<String>read(r.body(), "$.code")).isEqualTo("CLIENT_OVERLAP"));
        }
    }

    @Test
    void startThatIsNotAFreeSlotReturns409SlotTaken() throws Exception {
        Business setup = business();

        assertThat(book(registerClient(), UUID.randomUUID(), setup, setup.employeeId(), "2026-11-27T08:05:00Z", null))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("SLOT_TAKEN");
    }

    @Test
    void pgErrorsReadsStateAndConstraintNameOfRealViolations() throws Exception {
        Business setup = business();
        String otherEmployeeId = bookableEmployee(setup, "Bartek W.");
        UUID otherEmployee = UUID.fromString(otherEmployeeId);
        BookableOffering offering = offeringService.getBookable(UUID.fromString(setup.offeringId()));
        Appointment first = bookAndLoad(registerClient(), setup, setup.employeeId(), NINE_AM);
        Appointment second = bookAndLoad(registerClient(), setup, setup.employeeId(), TEN_FIFTEEN_AM);

        assertThat(violationOf(Appointment.book(second.getClientId(), first.getEmployeeId(), offering,
                        first.getStartTime(), null, UUID.randomUUID())))
                .contains(new Violation(PgErrors.EXCLUSION_VIOLATION, "ex_appointments_employee_overlap"));
        assertThat(violationOf(Appointment.book(first.getClientId(), otherEmployee, offering,
                        first.getStartTime(), null, UUID.randomUUID())))
                .contains(new Violation(PgErrors.EXCLUSION_VIOLATION, "ex_appointments_client_overlap"));
        assertThat(violationOf(Appointment.book(first.getClientId(), otherEmployee, offering,
                        Instant.parse(TEN_AM), null, first.getIdempotencyKey())))
                .contains(new Violation(PgErrors.UNIQUE_VIOLATION, "uk_appointments_client_idempotency_key"));
    }

    private record Business(Cookie owner, String slug, String offeringId, String employeeId) {
    }

    private record Response(int status, String body) {
    }

    private List<Response> inParallel(Callable<MvcTestResult> request) throws Exception {
        List<Callable<MvcTestResult>> requests = new ArrayList<>();
        for (int i = 0; i < PARALLEL_REQUESTS; i++) {
            requests.add(request);
        }
        return inParallel(requests);
    }

    private List<Response> inParallel(List<Callable<MvcTestResult>> requests) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(requests.size())) {
            List<Future<Response>> futures = new ArrayList<>();
            for (Callable<MvcTestResult> request : requests) {
                futures.add(executor.submit(() -> {
                    start.await();
                    MvcTestResult result = request.call();
                    return new Response(result.getResponse().getStatus(), result.getResponse().getContentAsString());
                }));
            }
            start.countDown();
            List<Response> responses = new ArrayList<>();
            for (Future<Response> future : futures) {
                responses.add(future.get());
            }
            return responses;
        }
    }

    private Optional<Violation> violationOf(Appointment conflicting) {
        DataIntegrityViolationException ex = catchThrowableOfType(
                DataIntegrityViolationException.class, () -> appointmentRepository.saveAndFlush(conflicting));
        assertThat(ex).isNotNull();
        return PgErrors.classify(ex);
    }

    private Appointment bookAndLoad(Cookie client, Business setup, String employeeId, String start) throws Exception {
        MvcTestResult booked = book(client, UUID.randomUUID(), setup, employeeId, start, null);
        assertThat(booked).hasStatus(HttpStatus.CREATED);
        return appointmentRepository.findById(UUID.fromString(idOf(booked))).orElseThrow();
    }

    private List<Appointment> appointmentsOf(String employeeId) {
        return appointmentsOf(UUID.fromString(employeeId));
    }

    private List<Appointment> appointmentsOf(UUID employeeId) {
        return appointmentRepository.findAll().stream()
                .filter(a -> a.getEmployeeId().equals(employeeId))
                .toList();
    }

    private MvcTestResult book(
            Cookie client, UUID key, Business setup, String employeeId, String start, String note) {
        String json = """
                {"offeringId": "%s", "employeeId": "%s", "startTime": "%s", "clientNote": %s}
                """.formatted(setup.offeringId(), employeeId, start, note == null ? "null" : "\"" + note + "\"");
        return mvc.post().uri(APPOINTMENTS_URL)
                .cookie(xsrf, client)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .header("Idempotency-Key", key.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private static String idOf(MvcTestResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private Cookie registerClient() {
        String registerJson = """
                {"email": "%s", "password": "Testowe123!", "firstName": "Ola",
                 "lastName": "Kowalska", "phone": "+48600123456"}
                """.formatted("booking-client-" + UUID.randomUUID() + "@test.pl");
        MvcTestResult registered = send(HttpMethod.POST, "/api/v1/auth/register", null, registerJson);
        assertThat(registered).hasStatus(HttpStatus.CREATED);
        return registered.getResponse().getCookie(SecurityConfig.ACCESS_TOKEN_COOKIE);
    }

    private Business business() throws Exception {
        Cookie clientToken = registerClient();
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

        Business withoutEmployee = new Business(owner, slug, offeringId, null);
        String employeeId = bookableEmployee(withoutEmployee, "Anna K.");
        return new Business(owner, slug, offeringId, employeeId);
    }

    private String bookableEmployee(Business setup, String displayName) throws Exception {
        MvcTestResult created = send(HttpMethod.POST, EMPLOYEES_URL, setup.owner(),
                "{\"displayName\": \"%s\"}".formatted(displayName));
        assertThat(created).hasStatus(HttpStatus.CREATED);
        String employeeId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        assertThat(send(HttpMethod.PUT, EMPLOYEES_URL + "/" + employeeId + "/offerings", setup.owner(),
                        "{\"offeringIds\": [\"%s\"]}".formatted(setup.offeringId())))
                .hasStatus(HttpStatus.OK);
        assertThat(send(HttpMethod.PUT, EMPLOYEES_URL + "/" + employeeId + "/schedule", setup.owner(), WEEK_JSON))
                .hasStatus(HttpStatus.OK);
        return employeeId;
    }

    private static String availabilityUrl(Business setup, String employeeId) {
        return "/api/v1/businesses/%s/availability?offeringId=%s&employeeId=%s&date=%s"
                .formatted(setup.slug(), setup.offeringId(), employeeId, FRIDAY);
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
