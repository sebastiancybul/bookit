package com.bookit.appointment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bookit.appointment.dto.AppointmentResponse;
import com.bookit.appointment.dto.CreateAppointmentRequest;
import com.bookit.appointment.entity.AppointmentStatus;
import com.bookit.appointment.service.BookingService;
import com.bookit.appointment.service.BookingService.BookingResult;
import com.bookit.auth.config.JwtConfig;
import com.bookit.auth.config.SecurityConfig;
import com.bookit.auth.controller.CsrfController;
import com.bookit.auth.security.ProblemDetailSecurityHandler;
import com.bookit.auth.security.TestTokens;
import com.bookit.shared.error.BusinessRuleException;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.UUID;
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

@WebMvcTest(controllers = {ClientAppointmentController.class, CsrfController.class})
@Import({SecurityConfig.class, JwtConfig.class, ProblemDetailSecurityHandler.class})
class ClientAppointmentControllerTest {

    private static final String URL = "/api/v1/appointments";
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID KEY = UUID.randomUUID();
    private static final UUID OFFERING_ID = UUID.randomUUID();
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();

    private static final String BOOKING_JSON = """
            {"offeringId": "%s", "employeeId": "%s", "startTime": "2026-11-27T08:00:00Z", "clientNote": "Hi"}
            """.formatted(OFFERING_ID, EMPLOYEE_ID);

    private static final AppointmentResponse APPOINTMENT = new AppointmentResponse(
            UUID.randomUUID(), OFFERING_ID, "Haircut", EMPLOYEE_ID,
            Instant.parse("2026-11-27T08:00:00Z"), Instant.parse("2026-11-27T08:30:00Z"),
            AppointmentStatus.CONFIRMED, 5000, "PLN", "Hi");

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JwtEncoder jwtEncoder;

    @MockitoBean
    BookingService bookingService;

    @MockitoBean
    UserDetailsService userDetailsService;

    @Test
    void anonymousGetsUnauthorized() {
        assertThat(send(null, KEY.toString(), BOOKING_JSON)).hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(bookingService);
    }

    @Test
    void withoutCsrfTokenGetsForbidden() {
        assertThat(mvc.post().uri(URL)
                        .cookie(clientToken())
                        .header("Idempotency-Key", KEY.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BOOKING_JSON))
                .hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(bookingService);
    }

    @Test
    void newBookingReturns201AndPassesTokenSubjectKeyAndBody() {
        given(bookingService.book(any(), any(), any())).willReturn(new BookingResult(APPOINTMENT, true));
        ArgumentCaptor<CreateAppointmentRequest> request = ArgumentCaptor.forClass(CreateAppointmentRequest.class);

        assertThat(send(clientToken(), KEY.toString(), BOOKING_JSON))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.offeringName").isEqualTo("Haircut");
                    assertThat(json).extractingPath("$.status").isEqualTo("CONFIRMED");
                });

        then(bookingService).should().book(eq(CLIENT_ID), eq(KEY), request.capture());
        assertThat(request.getValue().startTime()).isEqualTo(Instant.parse("2026-11-27T08:00:00Z"));
        assertThat(request.getValue().clientNote()).isEqualTo("Hi");
    }

    @Test
    void replayReturns200() {
        given(bookingService.book(any(), any(), any())).willReturn(new BookingResult(APPOINTMENT, false));

        assertThat(send(clientToken(), KEY.toString(), BOOKING_JSON))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.id")
                .isEqualTo(APPOINTMENT.id().toString());
    }

    @Test
    void missingIdempotencyKeyReturns400() {
        assertThat(send(clientToken(), null, BOOKING_JSON)).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(bookingService);
    }

    @Test
    void idempotencyKeyThatIsNotUuidReturns400() {
        assertThat(send(clientToken(), "abc", BOOKING_JSON)).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(bookingService);
    }

    @Test
    void invalidBodyReturns400WithFields() {
        String invalid = "{\"clientNote\": \"%s\"}".formatted("x".repeat(501));

        assertThat(send(clientToken(), KEY.toString(), invalid))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("offeringId", "employeeId", "startTime", "clientNote");
        verifyNoInteractions(bookingService);
    }

    @Test
    void takenSlotReturns409WithCode() {
        given(bookingService.book(any(), any(), any()))
                .willThrow(new BusinessRuleException("SLOT_TAKEN", "This time is no longer available"));

        assertThat(send(clientToken(), KEY.toString(), BOOKING_JSON))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("SLOT_TAKEN");
    }

    @Test
    void reusedKeyReturns422WithCode() {
        given(bookingService.book(any(), any(), any())).willThrow(new BusinessRuleException(
                HttpStatus.UNPROCESSABLE_CONTENT, "IDEMPOTENCY_KEY_REUSED", "Key already used"));

        assertThat(send(clientToken(), KEY.toString(), BOOKING_JSON))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("IDEMPOTENCY_KEY_REUSED");
    }

    private MvcTestResult send(Cookie token, String idempotencyKey, String json) {
        Cookie xsrf = mvc.get().uri("/api/v1/csrf").exchange().getResponse().getCookie("XSRF-TOKEN");
        var request = mvc.post().uri(URL)
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);
        if (token != null) {
            request = request.cookie(token);
        }
        if (idempotencyKey != null) {
            request = request.header("Idempotency-Key", idempotencyKey);
        }
        return request.exchange();
    }

    private Cookie clientToken() {
        return TestTokens.accessTokenCookie(jwtEncoder, CLIENT_ID, "ROLE_CLIENT");
    }
}
