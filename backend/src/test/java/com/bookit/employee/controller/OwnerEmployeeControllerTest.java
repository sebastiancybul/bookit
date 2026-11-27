package com.bookit.employee.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bookit.auth.config.JwtConfig;
import com.bookit.auth.config.SecurityConfig;
import com.bookit.auth.controller.CsrfController;
import com.bookit.auth.security.AuthCookies;
import com.bookit.auth.security.ProblemDetailSecurityHandler;
import com.bookit.auth.security.TestTokens;
import com.bookit.auth.service.AuthService;
import com.bookit.employee.dto.EmployeeRequest;
import com.bookit.employee.dto.EmployeeResponse;
import com.bookit.employee.service.EmployeeService;
import com.bookit.employee.service.EmployeeService.AccountChange;
import com.bookit.shared.error.BusinessRuleException;
import com.bookit.shared.error.InvalidFieldException;
import com.bookit.shared.error.ResourceNotFoundException;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(controllers = {OwnerEmployeeController.class, CsrfController.class})
@Import({SecurityConfig.class, JwtConfig.class, AuthCookies.class, ProblemDetailSecurityHandler.class})
class OwnerEmployeeControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final UUID OFFERING_A = UUID.randomUUID();
    private static final UUID OFFERING_B = UUID.randomUUID();
    private static final String BASE_URL = "/api/v1/owner/business/employees";
    private static final String OFFERINGS_URL = BASE_URL + "/" + EMPLOYEE_ID + "/offerings";
    private static final String ACCOUNT_URL = BASE_URL + "/" + EMPLOYEE_ID + "/account";

    private static final String EMPLOYEE_JSON = """
            {"displayName": "Anna K.", "position": "Senior barber"}
            """;

    private static final EmployeeResponse EMPLOYEE =
            new EmployeeResponse(EMPLOYEE_ID, "Anna K.", "Senior barber", List.of(), null);

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JwtEncoder jwtEncoder;

    @MockitoBean
    EmployeeService employeeService;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    UserDetailsService userDetailsService;

    @Test
    void anonymousGetsUnauthorized() {
        assertThat(mvc.get().uri(BASE_URL)).hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(employeeService);
    }

    @Test
    void clientWithoutOwnerRoleGetsForbidden() {
        assertThat(mvc.get().uri(BASE_URL).cookie(clientToken())).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(employeeService);
    }

    @Test
    void createWithoutCsrfTokenGetsForbidden() {
        assertThat(mvc.post().uri(BASE_URL)
                        .cookie(ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMPLOYEE_JSON))
                .hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(employeeService);
    }

    @Test
    void getMineReturnsEmployeesOfTokenSubject() {
        given(employeeService.getMine(USER_ID)).willReturn(List.of(
                new EmployeeResponse(EMPLOYEE_ID, "Anna K.", null, List.of(OFFERING_A), "anna@test.pl")));

        assertThat(mvc.get().uri(BASE_URL).cookie(ownerToken()))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$[0].displayName").isEqualTo("Anna K.");
                    assertThat(json).extractingPath("$[0].position").isNull();
                    assertThat(json).extractingPath("$[0].offeringIds[0]").isEqualTo(OFFERING_A.toString());
                    assertThat(json).extractingPath("$[0].accountEmail").isEqualTo("anna@test.pl");
                });
    }

    @Test
    void createReturns201AndPassesTokenSubjectAndRequestToService() {
        given(employeeService.create(any(), any())).willReturn(EMPLOYEE);
        ArgumentCaptor<EmployeeRequest> request = ArgumentCaptor.forClass(EmployeeRequest.class);

        assertThat(send(HttpMethod.POST, BASE_URL, EMPLOYEE_JSON))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.id")
                .isEqualTo(EMPLOYEE_ID.toString());

        then(employeeService).should().create(eq(USER_ID), request.capture());
        assertThat(request.getValue().displayName()).isEqualTo("Anna K.");
        assertThat(request.getValue().position()).isEqualTo("Senior barber");
    }

    @Test
    void createWithInvalidFieldsReturns400WithoutCallingService() {
        String invalid = """
                {"displayName": " ", "position": "%s"}
                """.formatted("x".repeat(81));

        assertThat(send(HttpMethod.POST, BASE_URL, invalid))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("displayName", "position");
        verifyNoInteractions(employeeService);
    }

    @Test
    void createWithoutPositionIsAccepted() {
        given(employeeService.create(any(), any())).willReturn(EMPLOYEE);

        assertThat(send(HttpMethod.POST, BASE_URL, "{\"displayName\": \"Anna K.\"}"))
                .hasStatus(HttpStatus.CREATED);
    }

    @Test
    void createWithDuplicateDisplayNameReturns409WithCode() {
        given(employeeService.create(any(), any()))
                .willThrow(new BusinessRuleException("EMPLOYEE_NAME_TAKEN", "An employee with this name already exists"));

        assertThat(send(HttpMethod.POST, BASE_URL, EMPLOYEE_JSON))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("EMPLOYEE_NAME_TAKEN");
    }

    @Test
    void updatePassesTokenSubjectAndPathIdToService() {
        given(employeeService.update(eq(USER_ID), eq(EMPLOYEE_ID), any())).willReturn(EMPLOYEE);

        assertThat(send(HttpMethod.PUT, BASE_URL + "/" + EMPLOYEE_ID, EMPLOYEE_JSON))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.displayName")
                .isEqualTo("Anna K.");
    }

    @Test
    void updateOfMissingOrForeignEmployeeReturns404() {
        given(employeeService.update(any(), any(), any()))
                .willThrow(new ResourceNotFoundException("Employee", EMPLOYEE_ID));

        assertThat(send(HttpMethod.PUT, BASE_URL + "/" + EMPLOYEE_ID, EMPLOYEE_JSON))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateWithMalformedIdReturns400() {
        assertThat(send(HttpMethod.PUT, BASE_URL + "/not-a-uuid", EMPLOYEE_JSON))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(employeeService);
    }

    @Test
    void assignOfferingsPassesDeduplicatedSetToService() {
        given(employeeService.assignOfferings(eq(USER_ID), eq(EMPLOYEE_ID), any()))
                .willReturn(new EmployeeResponse(EMPLOYEE_ID, "Anna K.", null, List.of(OFFERING_A, OFFERING_B), null));
        String json = """
                {"offeringIds": ["%s", "%s", "%s"]}
                """.formatted(OFFERING_A, OFFERING_B, OFFERING_A);

        assertThat(send(HttpMethod.PUT, OFFERINGS_URL, json))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.offeringIds")
                .asArray()
                .hasSize(2);

        then(employeeService).should().assignOfferings(USER_ID, EMPLOYEE_ID, Set.of(OFFERING_A, OFFERING_B));
    }

    @Test
    void assignEmptyListClearsAssignments() {
        given(employeeService.assignOfferings(USER_ID, EMPLOYEE_ID, Set.of())).willReturn(EMPLOYEE);

        assertThat(send(HttpMethod.PUT, OFFERINGS_URL, "{\"offeringIds\": []}"))
                .hasStatus(HttpStatus.OK);

        then(employeeService).should().assignOfferings(USER_ID, EMPLOYEE_ID, Set.of());
    }

    @Test
    void assignWithoutListReturns400() {
        assertThat(send(HttpMethod.PUT, OFFERINGS_URL, "{}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[0].field")
                .isEqualTo("offeringIds");
        verifyNoInteractions(employeeService);
    }

    @Test
    void assignWithNullElementReturns400() {
        assertThat(send(HttpMethod.PUT, OFFERINGS_URL, "{\"offeringIds\": [null]}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(employeeService);
    }

    @Test
    void assignWithMalformedIdReturns400() {
        assertThat(send(HttpMethod.PUT, OFFERINGS_URL, "{\"offeringIds\": [\"abc\"]}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(employeeService);
    }

    @Test
    void assignMoreThan100OfferingsReturns400() {
        String ids = IntStream.range(0, 101)
                .mapToObj(i -> "\"" + UUID.randomUUID() + "\"")
                .collect(Collectors.joining(","));

        assertThat(send(HttpMethod.PUT, OFFERINGS_URL, "{\"offeringIds\": [" + ids + "]}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(employeeService);
    }

    @Test
    void assignForeignOrUnknownOfferingReturns400OnField() {
        given(employeeService.assignOfferings(any(), any(), any()))
                .willThrow(new InvalidFieldException("offeringIds", "Unknown offering"));

        assertThat(send(HttpMethod.PUT, OFFERINGS_URL, "{\"offeringIds\": [\"" + OFFERING_A + "\"]}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[0].field")
                .isEqualTo("offeringIds");
    }

    @Test
    void linkAccountPassesEmailAndDoesNotTouchOwnCookieForAnotherUser() {
        EmployeeResponse linked = new EmployeeResponse(EMPLOYEE_ID, "Anna K.", null, List.of(), "anna@test.pl");
        given(employeeService.linkAccount(USER_ID, EMPLOYEE_ID, "anna@test.pl"))
                .willReturn(new AccountChange(linked, false));

        MvcTestResult result = send(HttpMethod.PUT, ACCOUNT_URL, "{\"email\": \"anna@test.pl\"}");

        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.accountEmail")
                .isEqualTo("anna@test.pl");
        assertThat(result.getResponse().getCookie(SecurityConfig.ACCESS_TOKEN_COOKIE)).isNull();
        then(authService).should(never()).reissueToken(any());
    }

    @Test
    void linkingOwnAccountReissuesOwnCookie() {
        given(employeeService.linkAccount(any(), any(), any())).willReturn(new AccountChange(EMPLOYEE, true));
        given(authService.reissueToken(USER_ID)).willReturn("new.jwt.value");

        MvcTestResult result = send(HttpMethod.PUT, ACCOUNT_URL, "{\"email\": \"owner@test.pl\"}");

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result.getResponse().getCookie(SecurityConfig.ACCESS_TOKEN_COOKIE).getValue())
                .isEqualTo("new.jwt.value");
    }

    @Test
    void linkWithInvalidEmailReturns400WithoutCallingService() {
        assertThat(send(HttpMethod.PUT, ACCOUNT_URL, "{\"email\": \"not-an-email\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[0].field")
                .isEqualTo("email");
        assertThat(send(HttpMethod.PUT, ACCOUNT_URL, "{}")).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(employeeService);
    }

    @Test
    void linkUnknownEmailReturns400OnField() {
        given(employeeService.linkAccount(any(), any(), any()))
                .willThrow(new InvalidFieldException("email", "No Bookit account with this email"));

        assertThat(send(HttpMethod.PUT, ACCOUNT_URL, "{\"email\": \"ghost@test.pl\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[0].field")
                .isEqualTo("email");
    }

    @Test
    void linkAccountTakenReturns409WithCode() {
        given(employeeService.linkAccount(any(), any(), any()))
                .willThrow(new BusinessRuleException("EMPLOYEE_ACCOUNT_TAKEN", "This account is already linked"));

        assertThat(send(HttpMethod.PUT, ACCOUNT_URL, "{\"email\": \"anna@test.pl\"}"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("EMPLOYEE_ACCOUNT_TAKEN");
    }

    @Test
    void unlinkReturns204AndReissuesCookieOnlyForOwnAccount() {
        given(employeeService.unlinkAccount(USER_ID, EMPLOYEE_ID)).willReturn(new AccountChange(EMPLOYEE, false));

        MvcTestResult other = send(HttpMethod.DELETE, ACCOUNT_URL, null);

        assertThat(other).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(other.getResponse().getCookie(SecurityConfig.ACCESS_TOKEN_COOKIE)).isNull();

        given(employeeService.unlinkAccount(USER_ID, EMPLOYEE_ID)).willReturn(new AccountChange(EMPLOYEE, true));
        given(authService.reissueToken(USER_ID)).willReturn("new.jwt.value");

        MvcTestResult own = send(HttpMethod.DELETE, ACCOUNT_URL, null);

        assertThat(own).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(own.getResponse().getCookie(SecurityConfig.ACCESS_TOKEN_COOKIE).getValue())
                .isEqualTo("new.jwt.value");
    }

    @Test
    void deleteReturns204() {
        assertThat(send(HttpMethod.DELETE, BASE_URL + "/" + EMPLOYEE_ID, null))
                .hasStatus(HttpStatus.NO_CONTENT);

        then(employeeService).should().delete(USER_ID, EMPLOYEE_ID);
    }

    @Test
    void deleteOfMissingOrForeignEmployeeReturns404() {
        willThrow(new ResourceNotFoundException("Employee", EMPLOYEE_ID))
                .given(employeeService).delete(any(), any());

        assertThat(send(HttpMethod.DELETE, BASE_URL + "/" + EMPLOYEE_ID, null))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteOfEmployeeWithUpcomingAppointmentsReturns409WithCode() {
        willThrow(new BusinessRuleException("EMPLOYEE_HAS_UPCOMING_APPOINTMENTS", "Has 2 upcoming appointment(s)"))
                .given(employeeService).delete(any(), any());

        assertThat(send(HttpMethod.DELETE, BASE_URL + "/" + EMPLOYEE_ID, null))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("EMPLOYEE_HAS_UPCOMING_APPOINTMENTS");
    }

    private MvcTestResult send(HttpMethod method, String uri, String json) {
        Cookie xsrf = mvc.get().uri("/api/v1/csrf").exchange().getResponse().getCookie("XSRF-TOKEN");
        var request = mvc.method(method).uri(uri)
                .cookie(xsrf, ownerToken())
                .header("X-XSRF-TOKEN", xsrf.getValue());
        if (json != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return request.exchange();
    }

    private Cookie clientToken() {
        return TestTokens.accessTokenCookie(jwtEncoder, USER_ID, "ROLE_CLIENT");
    }

    private Cookie ownerToken() {
        return TestTokens.accessTokenCookie(jwtEncoder, USER_ID, "ROLE_CLIENT", "ROLE_OWNER");
    }
}
