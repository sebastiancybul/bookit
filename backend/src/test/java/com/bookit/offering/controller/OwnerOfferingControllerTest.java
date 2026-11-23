package com.bookit.offering.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bookit.auth.config.JwtConfig;
import com.bookit.auth.config.SecurityConfig;
import com.bookit.auth.controller.CsrfController;
import com.bookit.auth.security.AuthCookies;
import com.bookit.auth.security.ProblemDetailSecurityHandler;
import com.bookit.auth.security.TestTokens;
import com.bookit.offering.dto.OfferingRequest;
import com.bookit.offering.dto.OfferingResponse;
import com.bookit.offering.service.OfferingService;
import com.bookit.shared.error.BusinessRuleException;
import com.bookit.shared.error.ResourceNotFoundException;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
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

@WebMvcTest(controllers = {OwnerOfferingController.class, CsrfController.class})
@Import({SecurityConfig.class, JwtConfig.class, AuthCookies.class, ProblemDetailSecurityHandler.class})
class OwnerOfferingControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OFFERING_ID = UUID.randomUUID();
    private static final String BASE_URL = "/api/v1/owner/business/offerings";

    private static final String OFFERING_JSON = """
            {"name": "Haircut", "description": "Classic cut", "priceAmount": 5000,
             "durationMinutes": 30, "bufferMinutes": 10}
            """;

    private static final OfferingResponse OFFERING =
            new OfferingResponse(OFFERING_ID, "Haircut", "Classic cut", 5000, "PLN", 30, 10);

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JwtEncoder jwtEncoder;

    @MockitoBean
    OfferingService offeringService;

    @MockitoBean
    UserDetailsService userDetailsService;

    @Test
    void anonymousGetsUnauthorized() {
        assertThat(mvc.get().uri(BASE_URL)).hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(offeringService);
    }

    @Test
    void clientWithoutOwnerRoleGetsForbidden() {
        assertThat(mvc.get().uri(BASE_URL).cookie(clientToken())).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(offeringService);
    }

    @Test
    void createWithoutCsrfTokenGetsForbidden() {
        assertThat(mvc.post().uri(BASE_URL)
                        .cookie(ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OFFERING_JSON))
                .hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(offeringService);
    }

    @Test
    void getMineReturnsOfferingsOfTokenSubject() {
        given(offeringService.getMine(USER_ID)).willReturn(List.of(OFFERING));

        assertThat(mvc.get().uri(BASE_URL).cookie(ownerToken()))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$[0].name")
                .isEqualTo("Haircut");
    }

    @Test
    void createReturns201AndPassesTokenSubjectAndRequestToService() {
        given(offeringService.create(any(), any())).willReturn(OFFERING);
        ArgumentCaptor<OfferingRequest> request = ArgumentCaptor.forClass(OfferingRequest.class);

        assertThat(send(HttpMethod.POST, BASE_URL, OFFERING_JSON))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.currency")
                .isEqualTo("PLN");

        then(offeringService).should().create(eq(USER_ID), request.capture());
        assertThat(request.getValue().priceAmount()).isEqualTo(5000L);
        assertThat(request.getValue().bufferMinutes()).isEqualTo(10);
    }

    @Test
    void createWithInvalidFieldsReturns400WithoutCallingService() {
        String invalid = """
                {"name": " ", "priceAmount": -1, "durationMinutes": 37, "bufferMinutes": 7}
                """;

        assertThat(send(HttpMethod.POST, BASE_URL, invalid))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("name", "priceAmount", "durationMinutes", "bufferMinutes");
        verifyNoInteractions(offeringService);
    }

    @Test
    void createWithMissingNumbersReturns400InsteadOfDefaultingToZero() {
        assertThat(send(HttpMethod.POST, BASE_URL, "{\"name\": \"Haircut\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("priceAmount", "durationMinutes", "bufferMinutes");
        verifyNoInteractions(offeringService);
    }

    @Test
    void createWithDuplicateNameReturns409WithCode() {
        given(offeringService.create(any(), any()))
                .willThrow(new BusinessRuleException("OFFERING_NAME_TAKEN", "An offering with this name already exists"));

        assertThat(send(HttpMethod.POST, BASE_URL, OFFERING_JSON))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("OFFERING_NAME_TAKEN");
    }

    @Test
    void updatePassesTokenSubjectAndPathIdToService() {
        given(offeringService.update(eq(USER_ID), eq(OFFERING_ID), any())).willReturn(OFFERING);

        assertThat(send(HttpMethod.PUT, BASE_URL + "/" + OFFERING_ID, OFFERING_JSON))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.id")
                .isEqualTo(OFFERING_ID.toString());
    }

    @Test
    void updateOfMissingOrForeignOfferingReturns404() {
        given(offeringService.update(any(), any(), any()))
                .willThrow(new ResourceNotFoundException("Offering", OFFERING_ID));

        assertThat(send(HttpMethod.PUT, BASE_URL + "/" + OFFERING_ID, OFFERING_JSON))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateWithMalformedIdReturns400() {
        assertThat(send(HttpMethod.PUT, BASE_URL + "/not-a-uuid", OFFERING_JSON))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(offeringService);
    }

    @Test
    void deleteReturns204() {
        assertThat(send(HttpMethod.DELETE, BASE_URL + "/" + OFFERING_ID, null))
                .hasStatus(HttpStatus.NO_CONTENT);

        then(offeringService).should().delete(USER_ID, OFFERING_ID);
    }

    @Test
    void deleteOfMissingOrForeignOfferingReturns404() {
        willThrow(new ResourceNotFoundException("Offering", OFFERING_ID))
                .given(offeringService).delete(any(), any());

        assertThat(send(HttpMethod.DELETE, BASE_URL + "/" + OFFERING_ID, null))
                .hasStatus(HttpStatus.NOT_FOUND);
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
