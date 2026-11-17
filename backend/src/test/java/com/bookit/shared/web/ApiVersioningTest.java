package com.bookit.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(ApiVersioningTest.VersionedController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiVersioningTest.VersionedController.class)
class ApiVersioningTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void supportedVersionReachesController() {
        assertThat(mvc.get().uri("/api/v1/test/ping"))
                .hasStatus(HttpStatus.OK)
                .bodyText()
                .isEqualTo("pong");
    }

    @Test
    void unsupportedVersionReturns400ProblemDetail() {
        assertThat(mvc.get().uri("/api/v2/test/ping"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .asString()
                .contains("Invalid API version");
    }

    @Test
    void pathWithoutPrefixIsNotMapped() {
        assertThat(mvc.get().uri("/test/ping")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void versionWithoutVPrefixIsNotMapped() {
        assertThat(mvc.get().uri("/api/1/test/ping")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @RestController
    static class VersionedController {

        @GetMapping(path = "/test/ping", version = "1")
        String ping() {
            return "pong";
        }
    }
}
