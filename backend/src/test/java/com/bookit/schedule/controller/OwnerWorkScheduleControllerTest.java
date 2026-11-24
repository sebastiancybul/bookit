package com.bookit.schedule.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bookit.auth.config.JwtConfig;
import com.bookit.auth.config.SecurityConfig;
import com.bookit.auth.controller.CsrfController;
import com.bookit.auth.security.AuthCookies;
import com.bookit.auth.security.ProblemDetailSecurityHandler;
import com.bookit.auth.security.TestTokens;
import com.bookit.schedule.dto.WorkScheduleRequest;
import com.bookit.schedule.dto.WorkScheduleResponse;
import com.bookit.schedule.dto.WorkingHoursIntervalDto;
import com.bookit.schedule.service.WorkScheduleService;
import com.bookit.shared.error.BusinessRuleException;
import com.bookit.shared.error.InvalidFieldException;
import com.bookit.shared.error.ResourceNotFoundException;
import jakarta.servlet.http.Cookie;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

@WebMvcTest(controllers = {OwnerWorkScheduleController.class, CsrfController.class})
@Import({SecurityConfig.class, JwtConfig.class, AuthCookies.class, ProblemDetailSecurityHandler.class})
class OwnerWorkScheduleControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final String URL = "/api/v1/owner/business/employees/" + EMPLOYEE_ID + "/schedule";

    private static final String WEEK_JSON = """
            {"intervals": [
              {"dayOfWeek": 1, "startTime": "09:00", "endTime": "13:00"},
              {"dayOfWeek": 1, "startTime": "14:00", "endTime": "17:30"}
            ]}
            """;

    private static final WorkScheduleResponse WEEK = new WorkScheduleResponse(List.of(
            new WorkingHoursIntervalDto(1, LocalTime.of(9, 0), LocalTime.of(13, 0)),
            new WorkingHoursIntervalDto(1, LocalTime.of(14, 0), LocalTime.of(17, 30))));

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JwtEncoder jwtEncoder;

    @MockitoBean
    WorkScheduleService workScheduleService;

    @MockitoBean
    UserDetailsService userDetailsService;

    @Test
    void anonymousGetsUnauthorized() {
        assertThat(mvc.get().uri(URL)).hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(workScheduleService);
    }

    @Test
    void clientWithoutOwnerRoleGetsForbidden() {
        assertThat(mvc.get().uri(URL).cookie(clientToken())).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(workScheduleService);
    }

    @Test
    void replaceWithoutCsrfTokenGetsForbidden() {
        assertThat(mvc.put().uri(URL)
                        .cookie(ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(WEEK_JSON))
                .hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(workScheduleService);
    }

    @Test
    void getReturnsWeekWithTimesAsHoursAndMinutes() {
        given(workScheduleService.get(USER_ID, EMPLOYEE_ID)).willReturn(WEEK);

        assertThat(mvc.get().uri(URL).cookie(ownerToken()))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.intervals[0].dayOfWeek").isEqualTo(1);
                    assertThat(json).extractingPath("$.intervals[0].startTime").isEqualTo("09:00");
                    assertThat(json).extractingPath("$.intervals[1].endTime").isEqualTo("17:30");
                });
    }

    @Test
    void replacePassesTokenSubjectEmployeeAndParsedWeekToService() {
        given(workScheduleService.replace(any(), any(), any())).willReturn(WEEK);
        ArgumentCaptor<WorkScheduleRequest> request = ArgumentCaptor.forClass(WorkScheduleRequest.class);

        assertThat(put(WEEK_JSON))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.intervals")
                .asArray()
                .hasSize(2);

        then(workScheduleService).should().replace(eq(USER_ID), eq(EMPLOYEE_ID), request.capture());
        assertThat(request.getValue().intervals()).containsExactly(
                new WorkingHoursIntervalDto(1, LocalTime.of(9, 0), LocalTime.of(13, 0)),
                new WorkingHoursIntervalDto(1, LocalTime.of(14, 0), LocalTime.of(17, 30)));
    }

    @Test
    void emptyWeekIsAccepted() {
        given(workScheduleService.replace(any(), any(), any())).willReturn(new WorkScheduleResponse(List.of()));

        assertThat(put("{\"intervals\": []}")).hasStatus(HttpStatus.OK);
    }

    @Test
    void invalidIntervalFieldsReturn400WithPathsWithoutCallingService() {
        String invalid = """
                {"intervals": [
                  {"dayOfWeek": 8, "startTime": "09:00", "endTime": "13:00"},
                  {"dayOfWeek": 2, "startTime": "09:00"}
                ]}
                """;

        assertThat(put(invalid))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("intervals[0].dayOfWeek", "intervals[1].endTime");
        verifyNoInteractions(workScheduleService);
    }

    @Test
    void missingIntervalsReturns400() {
        assertThat(put("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[0].field")
                .isEqualTo("intervals");
        verifyNoInteractions(workScheduleService);
    }

    @Test
    void moreThanFiftyIntervalsReturns400() {
        String intervals = IntStream.range(0, 51)
                .mapToObj(i -> "{\"dayOfWeek\": 1, \"startTime\": \"09:00\", \"endTime\": \"10:00\"}")
                .collect(Collectors.joining(","));

        assertThat(put("{\"intervals\": [" + intervals + "]}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[0].field")
                .isEqualTo("intervals");
        verifyNoInteractions(workScheduleService);
    }

    @Test
    void timeNotInHoursAndMinutesFormatReturns400() {
        assertThat(put("{\"intervals\": [{\"dayOfWeek\": 1, \"startTime\": \"09:00:30\", \"endTime\": \"10:00\"}]}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(workScheduleService);
    }

    @Test
    void domainValidationErrorFromServiceReturns400() {
        given(workScheduleService.replace(any(), any(), any()))
                .willThrow(new InvalidFieldException("intervals", "Working hours overlap on Monday"));

        assertThat(put(WEEK_JSON))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[0].message")
                .isEqualTo("Working hours overlap on Monday");
    }

    @Test
    void foreignOrUnknownEmployeeReturns404() {
        given(workScheduleService.get(USER_ID, EMPLOYEE_ID))
                .willThrow(new ResourceNotFoundException("Employee", EMPLOYEE_ID));

        assertThat(mvc.get().uri(URL).cookie(ownerToken())).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void concurrentUpdateReturns409() {
        given(workScheduleService.replace(any(), any(), any()))
                .willThrow(new BusinessRuleException("CONCURRENT_UPDATE", "Changed at the same time"));

        assertThat(put(WEEK_JSON))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("CONCURRENT_UPDATE");
    }

    private MvcTestResult put(String json) {
        Cookie xsrf = mvc.get().uri("/api/v1/csrf").exchange().getResponse().getCookie("XSRF-TOKEN");
        return mvc.put().uri(URL)
                .cookie(xsrf, ownerToken())
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private Cookie clientToken() {
        return TestTokens.accessTokenCookie(jwtEncoder, USER_ID, "ROLE_CLIENT");
    }

    private Cookie ownerToken() {
        return TestTokens.accessTokenCookie(jwtEncoder, USER_ID, "ROLE_CLIENT", "ROLE_OWNER");
    }
}
