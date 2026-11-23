package com.bookit.offering.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.bookit.auth.config.JwtConfig;
import com.bookit.auth.config.SecurityConfig;
import com.bookit.auth.security.ProblemDetailSecurityHandler;
import com.bookit.offering.dto.PublicOfferingResponse;
import com.bookit.offering.service.OfferingService;
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

@WebMvcTest(controllers = PublicOfferingController.class)
@Import({SecurityConfig.class, JwtConfig.class, ProblemDetailSecurityHandler.class})
class PublicOfferingControllerTest {

    private static final String SLUG = "k3f9x2ab-barber-shop-krakow";
    private static final String URL = "/api/v1/businesses/" + SLUG + "/offerings";

    private static final PublicOfferingResponse HAIRCUT =
            new PublicOfferingResponse(UUID.randomUUID(), "Haircut", null, 5000, "PLN", 30);

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    OfferingService offeringService;

    @MockitoBean
    UserDetailsService userDetailsService;

    @Test
    void anonymousGetsOfferingsWithoutBuffer() {
        given(offeringService.getPublic(SLUG)).willReturn(List.of(HAIRCUT));

        assertThat(mvc.get().uri(URL))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$[0].name").isEqualTo("Haircut");
                    assertThat(json).extractingPath("$[0].durationMinutes").isEqualTo(30);
                    assertThat(json).doesNotHavePath("$[0].bufferMinutes");
                });
    }

    @Test
    void unknownOrInactiveBusinessReturns404() {
        given(offeringService.getPublic(SLUG)).willThrow(new ResourceNotFoundException("Business", SLUG));

        assertThat(mvc.get().uri(URL)).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void invalidTokenCookieIsIgnored() {
        given(offeringService.getPublic(SLUG)).willReturn(List.of());

        assertThat(mvc.get().uri(URL).cookie(new Cookie(SecurityConfig.ACCESS_TOKEN_COOKIE, "not-a-jwt")))
                .hasStatus(HttpStatus.OK);
    }
}
