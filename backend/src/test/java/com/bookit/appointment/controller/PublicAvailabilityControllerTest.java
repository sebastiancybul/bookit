package com.bookit.appointment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.bookit.appointment.dto.AvailableSlotResponse;
import com.bookit.appointment.service.AvailabilityService;
import com.bookit.auth.config.JwtConfig;
import com.bookit.auth.config.SecurityConfig;
import com.bookit.auth.security.ProblemDetailSecurityHandler;
import com.bookit.shared.error.ResourceNotFoundException;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(controllers = PublicAvailabilityController.class)
@Import({SecurityConfig.class, JwtConfig.class, ProblemDetailSecurityHandler.class})
class PublicAvailabilityControllerTest {

    private static final String SLUG = "k3f9x2ab-barber-shop-krakow";
    private static final String URL = "/api/v1/businesses/" + SLUG + "/availability";
    private static final UUID OFFERING_ID = UUID.randomUUID();
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final LocalDate DATE = LocalDate.of(2026, 11, 26);

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    AvailabilityService availabilityService;

    @MockitoBean
    UserDetailsService userDetailsService;

    @Test
    void anonymousGetsSlotsWithUtcStartAndLocalTime() {
        given(availabilityService.getAvailableSlots(SLUG, OFFERING_ID, EMPLOYEE_ID, DATE)).willReturn(List.of(
                new AvailableSlotResponse(Instant.parse("2026-11-26T08:00:00Z"), LocalTime.of(9, 0))));

        assertThat(mvc.get().uri(URL)
                .param("offeringId", OFFERING_ID.toString())
                .param("employeeId", EMPLOYEE_ID.toString())
                .param("date", "2026-11-26"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$[0].startTime").isEqualTo("2026-11-26T08:00:00Z");
                    assertThat(json).extractingPath("$[0].localTime").isEqualTo("09:00");
                });
    }

    @Test
    void missingParameterReturns400() {
        assertThat(mvc.get().uri(URL)
                .param("offeringId", OFFERING_ID.toString())
                .param("employeeId", EMPLOYEE_ID.toString()))
                .hasStatus(HttpStatus.BAD_REQUEST);
        then(availabilityService).should(never()).getAvailableSlots(any(), any(), any(), any());
    }

    @Test
    void dateNotInIsoFormatReturns400() {
        assertThat(mvc.get().uri(URL)
                .param("offeringId", OFFERING_ID.toString())
                .param("employeeId", EMPLOYEE_ID.toString())
                .param("date", "26.11.2026"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        then(availabilityService).should(never()).getAvailableSlots(any(), any(), any(), any());
    }

    @Test
    void malformedIdReturns400() {
        assertThat(mvc.get().uri(URL)
                .param("offeringId", "abc")
                .param("employeeId", EMPLOYEE_ID.toString())
                .param("date", "2026-11-26"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unknownBusinessOfferingOrEmployeeReturns404() {
        given(availabilityService.getAvailableSlots(SLUG, OFFERING_ID, EMPLOYEE_ID, DATE))
                .willThrow(new ResourceNotFoundException("Employee", EMPLOYEE_ID));

        assertThat(mvc.get().uri(URL)
                .param("offeringId", OFFERING_ID.toString())
                .param("employeeId", EMPLOYEE_ID.toString())
                .param("date", "2026-11-26"))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void invalidTokenCookieIsIgnored() {
        given(availabilityService.getAvailableSlots(SLUG, OFFERING_ID, EMPLOYEE_ID, DATE)).willReturn(List.of());

        assertThat(mvc.get().uri(URL)
                .param("offeringId", OFFERING_ID.toString())
                .param("employeeId", EMPLOYEE_ID.toString())
                .param("date", "2026-11-26")
                .cookie(new Cookie(SecurityConfig.ACCESS_TOKEN_COOKIE, "not-a-jwt")))
                .hasStatus(HttpStatus.OK);
    }
}
