package com.bookit.business.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bookit.auth.config.JwtConfig;
import com.bookit.auth.config.SecurityConfig;
import com.bookit.auth.security.ProblemDetailSecurityHandler;
import com.bookit.business.dto.BusinessCategoryResponse;
import com.bookit.business.dto.BusinessResponse;
import com.bookit.business.dto.BusinessSummaryResponse;
import com.bookit.business.service.BusinessService;
import com.bookit.shared.error.ResourceNotFoundException;
import com.bookit.shared.web.PageResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(controllers = {PublicBusinessController.class, BusinessCategoryController.class})
@Import({SecurityConfig.class, JwtConfig.class, ProblemDetailSecurityHandler.class})
class PublicBusinessControllerTest {

    private static final BusinessCategoryResponse BARBER = new BusinessCategoryResponse("BARBER", "Barber");

    private static final BusinessSummaryResponse SUMMARY = new BusinessSummaryResponse(
            "k3f9x2ab", "k3f9x2ab-barber-shop-krakow", "Barber Shop", BARBER, "Kraków", "Floriańska 1");

    private static final BusinessResponse BUSINESS = new BusinessResponse(
            "k3f9x2ab",
            "k3f9x2ab-barber-shop-krakow",
            "Barber Shop",
            null,
            BARBER,
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

    @MockitoBean
    BusinessService businessService;

    @MockitoBean
    UserDetailsService userDetailsService;

    @Test
    void searchWithoutParamsUsesDefaultsAndNeedsNoLogin() {
        given(businessService.searchPublic(null, null, 0, 20))
                .willReturn(new PageResponse<>(List.of(SUMMARY), 0, 20, 1, 1));

        assertThat(mvc.get().uri("/api/v1/businesses"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.content[0].slug").isEqualTo("k3f9x2ab-barber-shop-krakow");
                    assertThat(json).extractingPath("$.totalElements").isEqualTo(1);
                });
    }

    @Test
    void searchPassesFiltersAndPagingToService() {
        given(businessService.searchPublic("Kraków", "BARBER", 2, 10))
                .willReturn(new PageResponse<>(List.of(), 2, 10, 0, 0));

        assertThat(mvc.get().uri("/api/v1/businesses")
                        .param("city", "Kraków")
                        .param("category", "BARBER")
                        .param("page", "2")
                        .param("size", "10"))
                .hasStatus(HttpStatus.OK);
        then(businessService).should().searchPublic("Kraków", "BARBER", 2, 10);
    }

    @Test
    void searchWithTooLargePageSizeReturns400() {
        assertThat(mvc.get().uri("/api/v1/businesses").param("size", "51")).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(businessService);
    }

    @Test
    void searchWithNegativePageReturns400() {
        assertThat(mvc.get().uri("/api/v1/businesses").param("page", "-1")).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(businessService);
    }

    @Test
    void searchWithTooLargePageReturns400() {
        assertThat(mvc.get().uri("/api/v1/businesses").param("page", "10001")).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(businessService);
    }

    @Test
    void searchWithTooLongCityReturns400() {
        assertThat(mvc.get().uri("/api/v1/businesses").param("city", "x".repeat(81)))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(businessService);
    }

    @Test
    void getBySlugReturnsBusiness() {
        given(businessService.getPublic("k3f9x2ab-old-name")).willReturn(BUSINESS);

        assertThat(mvc.get().uri("/api/v1/businesses/k3f9x2ab-old-name"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.slug")
                .isEqualTo("k3f9x2ab-barber-shop-krakow");
    }

    @Test
    void getBySlugOfUnknownBusinessReturns404() {
        given(businessService.getPublic("zzzzzzzz")).willThrow(new ResourceNotFoundException("Business", "zzzzzzzz"));

        assertThat(mvc.get().uri("/api/v1/businesses/zzzzzzzz")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void categoriesAreListedWithoutLogin() {
        given(businessService.getCategories()).willReturn(List.of(BARBER));

        assertThat(mvc.get().uri("/api/v1/categories"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$[0].code")
                .isEqualTo("BARBER");
    }
}
