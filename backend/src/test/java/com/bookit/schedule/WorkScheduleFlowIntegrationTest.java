package com.bookit.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookit.TestcontainersConfiguration;
import com.bookit.auth.config.SecurityConfig;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
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
class WorkScheduleFlowIntegrationTest {

    private static final String EMPLOYEES_URL = "/api/v1/owner/business/employees";

    private static final String WEEK_JSON = """
            {"intervals": [
              {"dayOfWeek": 2, "startTime": "09:00", "endTime": "17:00"},
              {"dayOfWeek": 1, "startTime": "14:00", "endTime": "18:00"},
              {"dayOfWeek": 1, "startTime": "08:00", "endTime": "12:00"}
            ]}
            """;

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    Cookie xsrf;

    @BeforeEach
    void fetchXsrfCookie() {
        xsrf = mvc.get().uri("/api/v1/csrf").exchange().getResponse().getCookie("XSRF-TOKEN");
    }

    @Test
    void ownerSetsAndReadsWeekSortedByDayAndStart() throws Exception {
        Cookie owner = becomeOwner();
        String annaId = createEmployee(owner, "Anna K.");

        assertThat(mvc.get().uri(scheduleUrl(annaId)).cookie(owner))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.intervals")
                .asArray()
                .isEmpty();
        assertThat(countSchedules(annaId)).isZero();

        assertThat(send(HttpMethod.PUT, scheduleUrl(annaId), owner, WEEK_JSON))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.intervals[*].startTime")
                .asArray()
                .containsExactly("08:00", "14:00", "09:00");

        assertThat(mvc.get().uri(scheduleUrl(annaId)).cookie(owner))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.intervals[*].dayOfWeek").asArray()
                            .containsExactly(1, 1, 2);
                    assertThat(json).extractingPath("$.intervals[*].startTime").asArray()
                            .containsExactly("08:00", "14:00", "09:00");
                    assertThat(json).extractingPath("$.intervals[*].endTime").asArray()
                            .containsExactly("12:00", "18:00", "17:00");
                });
    }

    @Test
    void secondPutReplacesWholeWeekAndBumpsVersion() throws Exception {
        Cookie owner = becomeOwner();
        String annaId = createEmployee(owner, "Anna K.");
        send(HttpMethod.PUT, scheduleUrl(annaId), owner, WEEK_JSON);
        assertThat(scheduleVersion(annaId)).isZero();

        assertThat(send(HttpMethod.PUT, scheduleUrl(annaId), owner,
                        "{\"intervals\": [{\"dayOfWeek\": 5, \"startTime\": \"10:00\", \"endTime\": \"14:00\"}]}"))
                .hasStatus(HttpStatus.OK);

        assertThat(intervalRows(annaId)).isEqualTo(1);
        assertThat(scheduleVersion(annaId)).isEqualTo(1);

        assertThat(send(HttpMethod.PUT, scheduleUrl(annaId), owner, "{\"intervals\": []}"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.intervals")
                .asArray()
                .isEmpty();
        assertThat(intervalRows(annaId)).isZero();
        assertThat(countSchedules(annaId)).isEqualTo(1);
    }

    @Test
    void overlappingIntervalsReturn400AndKeepPreviousWeek() throws Exception {
        Cookie owner = becomeOwner();
        String annaId = createEmployee(owner, "Anna K.");
        send(HttpMethod.PUT, scheduleUrl(annaId), owner, WEEK_JSON);

        String overlapping = """
                {"intervals": [
                  {"dayOfWeek": 3, "startTime": "09:00", "endTime": "12:00"},
                  {"dayOfWeek": 3, "startTime": "11:00", "endTime": "14:00"}
                ]}
                """;
        assertThat(send(HttpMethod.PUT, scheduleUrl(annaId), owner, overlapping))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.errors[0].field").isEqualTo("intervals");
                    assertThat(json).extractingPath("$.errors[0].message")
                            .isEqualTo("Working hours overlap on Wednesday");
                });

        assertThat(intervalRows(annaId)).isEqualTo(3);
        assertThat(scheduleVersion(annaId)).isZero();
    }

    @Test
    void scheduleOfDeletedOrUnknownEmployeeReturns404() throws Exception {
        Cookie owner = becomeOwner();
        String annaId = createEmployee(owner, "Anna K.");
        send(HttpMethod.DELETE, EMPLOYEES_URL + "/" + annaId, owner, null);

        assertThat(mvc.get().uri(scheduleUrl(annaId)).cookie(owner)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send(HttpMethod.PUT, scheduleUrl(annaId), owner, WEEK_JSON)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri(scheduleUrl(UUID.randomUUID().toString())).cookie(owner))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void ownerCannotReadOrReplaceScheduleOfAnotherBusinessEmployee() throws Exception {
        Cookie ownerA = becomeOwner();
        Cookie ownerB = becomeOwner();
        String employeeOfA = createEmployee(ownerA, "Anna K.");
        String employeeOfAWithoutSchedule = createEmployee(ownerA, "Bartek W.");
        send(HttpMethod.PUT, scheduleUrl(employeeOfA), ownerA, WEEK_JSON);

        assertThat(mvc.get().uri(scheduleUrl(employeeOfA)).cookie(ownerB)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send(HttpMethod.PUT, scheduleUrl(employeeOfA), ownerB, "{\"intervals\": []}"))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send(HttpMethod.PUT, scheduleUrl(employeeOfAWithoutSchedule), ownerB, WEEK_JSON))
                .hasStatus(HttpStatus.NOT_FOUND);

        assertThat(mvc.get().uri(scheduleUrl(employeeOfA)).cookie(ownerA))
                .bodyJson()
                .extractingPath("$.intervals")
                .asArray()
                .hasSize(3);
        assertThat(scheduleVersion(employeeOfA)).isZero();
        assertThat(countSchedules(employeeOfAWithoutSchedule)).isZero();
    }

    @Test
    void intervalsAreReadInChronologicalOrderWhateverTheRowOrder() throws Exception {
        Cookie owner = becomeOwner();
        String annaId = createEmployee(owner, "Anna K.");
        send(HttpMethod.PUT, scheduleUrl(annaId), owner, "{\"intervals\": []}");
        String scheduleId = jdbcTemplate.queryForObject(
                "SELECT id::text FROM work_schedules WHERE employee_id = ?::uuid", String.class, annaId);
        String insert = "INSERT INTO work_schedule_intervals VALUES (?::uuid, ?, ?::time, ?::time)";
        jdbcTemplate.update(insert, scheduleId, 3, "09:00", "12:00");
        jdbcTemplate.update(insert, scheduleId, 1, "14:00", "18:00");
        jdbcTemplate.update(insert, scheduleId, 1, "08:00", "12:00");

        assertThat(mvc.get().uri(scheduleUrl(annaId)).cookie(owner))
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.intervals[*].dayOfWeek").asArray()
                            .containsExactly(1, 1, 3);
                    assertThat(json).extractingPath("$.intervals[*].startTime").asArray()
                            .containsExactly("08:00", "14:00", "09:00");
                });
    }

    @Test
    void databaseRejectsInvalidDayAndTimeRange() throws Exception {
        Cookie owner = becomeOwner();
        String annaId = createEmployee(owner, "Anna K.");
        send(HttpMethod.PUT, scheduleUrl(annaId), owner, "{\"intervals\": []}");
        String scheduleId = jdbcTemplate.queryForObject(
                "SELECT id::text FROM work_schedules WHERE employee_id = ?::uuid", String.class, annaId);
        String insert = "INSERT INTO work_schedule_intervals VALUES (?::uuid, ?, ?::time, ?::time)";

        assertThatThrownBy(() -> jdbcTemplate.update(insert, scheduleId, 8, "09:00", "10:00"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_work_schedule_intervals_day_of_week");
        assertThatThrownBy(() -> jdbcTemplate.update(insert, scheduleId, 1, "10:00", "10:00"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_work_schedule_intervals_time_range");
    }

    private String scheduleUrl(String employeeId) {
        return EMPLOYEES_URL + "/" + employeeId + "/schedule";
    }

    private Integer countSchedules(String employeeId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM work_schedules WHERE employee_id = ?::uuid", Integer.class, employeeId);
    }

    private Long scheduleVersion(String employeeId) {
        return jdbcTemplate.queryForObject(
                "SELECT version FROM work_schedules WHERE employee_id = ?::uuid", Long.class, employeeId);
    }

    private Integer intervalRows(String employeeId) {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) FROM work_schedule_intervals i
                JOIN work_schedules s ON s.id = i.work_schedule_id
                WHERE s.employee_id = ?::uuid
                """, Integer.class, employeeId);
    }

    private Cookie becomeOwner() throws Exception {
        String registerJson = """
                {"email": "%s", "password": "Testowe123!", "firstName": "Anna",
                 "lastName": "Nowak", "phone": "+48600123456"}
                """.formatted("schedule-owner-" + UUID.randomUUID() + "@test.pl");
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

    private String createEmployee(Cookie owner, String displayName) throws Exception {
        MvcTestResult created = send(HttpMethod.POST, EMPLOYEES_URL, owner,
                "{\"displayName\": \"%s\"}".formatted(displayName));
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
}
