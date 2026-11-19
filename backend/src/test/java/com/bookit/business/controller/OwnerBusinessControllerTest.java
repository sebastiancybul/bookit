package com.bookit.business.controller;

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
import com.bookit.business.dto.BusinessCategoryResponse;
import com.bookit.business.dto.BusinessRequest;
import com.bookit.business.dto.BusinessResponse;
import com.bookit.business.service.BusinessService;
import com.bookit.shared.error.BusinessRuleException;
import com.bookit.shared.error.InvalidFieldException;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(controllers = {OwnerBusinessController.class, CsrfController.class})
@Import({SecurityConfig.class, JwtConfig.class, AuthCookies.class, ProblemDetailSecurityHandler.class})
class OwnerBusinessControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private static final String BUSINESS_JSON = """
            {"name": "Barber Shop", "categoryCode": "BARBER", "phone": "123456789",
             "street": "Floriańska 1", "city": "Kraków", "postalCode": "31-019"}
            """;

    private static final BusinessResponse BUSINESS = new BusinessResponse(
            "k3f9x2ab",
            "k3f9x2ab-barber-shop-krakow",
            "Barber Shop",
            null,
            new BusinessCategoryResponse("BARBER", "Barber"),
            "123456789",
            null,
            "Floriańska 1",
            "Kraków",
            "31-019",
            "Europe/Warsaw",
            24,
            15);

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JwtEncoder jwtEncoder;

    @MockitoBean
    BusinessService businessService;

    @MockitoBean
    UserDetailsService userDetailsService;

    @Test
    void createReturns201WithBusinessAndNewAccessTokenCookie() {
        given(businessService.create(eq(USER_ID), any()))
                .willReturn(new BusinessService.CreateResult(BUSINESS, "new.jwt.value"));

        MvcTestResult result = send(HttpMethod.POST, clientToken(), BUSINESS_JSON);

        assertThat(result).hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.slug")
                .isEqualTo("k3f9x2ab-barber-shop-krakow");
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .startsWith("access_token=new.jwt.value")
                .contains("HttpOnly");
    }

    @Test
    void createPassesTokenSubjectAndRequestToService() {
        given(businessService.create(any(), any()))
                .willReturn(new BusinessService.CreateResult(BUSINESS, "new.jwt.value"));
        ArgumentCaptor<BusinessRequest> request = ArgumentCaptor.forClass(BusinessRequest.class);

        send(HttpMethod.POST, clientToken(), BUSINESS_JSON);

        then(businessService).should().create(eq(USER_ID), request.capture());
        assertThat(request.getValue().name()).isEqualTo("Barber Shop");
        assertThat(request.getValue().categoryCode()).isEqualTo("BARBER");
    }

    @Test
    void createWithInvalidFieldsReturns400WithoutCallingService() {
        String invalid = BUSINESS_JSON.replace("\"Barber Shop\"", "\"\"").replace("31-019", "31019");

        assertThat(send(HttpMethod.POST, clientToken(), invalid))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("name", "postalCode");
        verifyNoInteractions(businessService);
    }

    @Test
    void createWithUnknownCategoryReturns400WithFieldError() {
        given(businessService.create(any(), any()))
                .willThrow(new InvalidFieldException("categoryCode", "Unknown category"));

        assertThat(send(HttpMethod.POST, clientToken(), BUSINESS_JSON))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[0].field")
                .isEqualTo("categoryCode");
    }

    @Test
    void createWhenBusinessExistsReturns409WithCode() {
        given(businessService.create(any(), any()))
                .willThrow(new BusinessRuleException("BUSINESS_ALREADY_EXISTS", "You already have a business"));

        assertThat(send(HttpMethod.POST, clientToken(), BUSINESS_JSON))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("BUSINESS_ALREADY_EXISTS");
    }

    @Test
    void getMineReturnsBusinessOfTokenSubject() {
        given(businessService.getMine(USER_ID)).willReturn(BUSINESS);

        assertThat(mvc.get().uri("/api/v1/owner/business").cookie(ownerToken()))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.category.code")
                .isEqualTo("BARBER");
    }

    @Test
    void updateMineReturnsUpdatedBusiness() {
        given(businessService.updateMine(eq(USER_ID), any())).willReturn(BUSINESS);

        assertThat(send(HttpMethod.PUT, ownerToken(), BUSINESS_JSON))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.publicId")
                .isEqualTo("k3f9x2ab");
    }

    @Test
    void updateIgnoresOwnerIdSentInBody() {
        given(businessService.updateMine(any(), any())).willReturn(BUSINESS);
        String withForeignOwner = BUSINESS_JSON.replace("{", "{\"ownerId\": \"" + UUID.randomUUID() + "\", ");

        assertThat(send(HttpMethod.PUT, ownerToken(), withForeignOwner)).hasStatus(HttpStatus.OK);

        then(businessService).should().updateMine(eq(USER_ID), any());
    }

    private MvcTestResult send(HttpMethod method, Cookie token, String json) {
        Cookie xsrf = mvc.get().uri("/api/v1/csrf").exchange().getResponse().getCookie("XSRF-TOKEN");
        return mvc.method(method).uri("/api/v1/owner/business")
                .cookie(xsrf, token)
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
