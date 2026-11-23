package com.bookit.employee.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.bookit.auth.config.JwtConfig;
import com.bookit.auth.config.SecurityConfig;
import com.bookit.auth.security.ProblemDetailSecurityHandler;
import com.bookit.employee.dto.PublicEmployeeResponse;
import com.bookit.employee.service.EmployeeService;
import com.bookit.shared.error.ResourceNotFoundException;
import jakarta.servlet.http.Cookie;
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

@WebMvcTest(controllers = PublicEmployeeController.class)
@Import({SecurityConfig.class, JwtConfig.class, ProblemDetailSecurityHandler.class})
class PublicEmployeeControllerTest {

    private static final String SLUG = "k3f9x2ab-barber-shop-krakow";
    private static final String URL = "/api/v1/businesses/" + SLUG + "/employees";
    private static final UUID OFFERING_ID = UUID.randomUUID();

    private static final PublicEmployeeResponse ANNA =
            new PublicEmployeeResponse(UUID.randomUUID(), "Anna K.", "Senior barber", List.of(OFFERING_ID));

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    EmployeeService employeeService;

    @MockitoBean
    UserDetailsService userDetailsService;

    @Test
    void anonymousGetsEmployeesWithOfferingIds() {
        given(employeeService.getPublic(SLUG)).willReturn(List.of(ANNA));

        assertThat(mvc.get().uri(URL))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$[0].displayName").isEqualTo("Anna K.");
                    assertThat(json).extractingPath("$[0].position").isEqualTo("Senior barber");
                    assertThat(json).extractingPath("$[0].offeringIds[0]").isEqualTo(OFFERING_ID.toString());
                });
    }

    @Test
    void unknownOrInactiveBusinessReturns404() {
        given(employeeService.getPublic(SLUG)).willThrow(new ResourceNotFoundException("Business", SLUG));

        assertThat(mvc.get().uri(URL)).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void invalidTokenCookieIsIgnored() {
        given(employeeService.getPublic(SLUG)).willReturn(List.of());

        assertThat(mvc.get().uri(URL).cookie(new Cookie(SecurityConfig.ACCESS_TOKEN_COOKIE, "not-a-jwt")))
                .hasStatus(HttpStatus.OK);
    }

    @Test
    void nestedPathBelowEmployeesIsNotPublic() {
        assertThat(mvc.get().uri(URL + "/" + UUID.randomUUID())).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
