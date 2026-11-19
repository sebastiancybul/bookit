package com.bookit.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(GlobalExceptionHandlerTest.ThrowingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandlerTest.ThrowingController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void resourceNotFoundReturns404ProblemDetail() {
        assertThat(mvc.get().uri("/api/v1/test/not-found"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Business with id 42 not found");
    }

    @Test
    void businessRuleReturns409WithCode() {
        assertThat(mvc.get().uri("/api/v1/test/business-rule"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("SLOT_TAKEN");
    }

    @Test
    void validationErrorReturns400WithFieldList() {
        assertThat(mvc.post()
                        .uri("/api/v1/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[0].field")
                .isEqualTo("name");
    }

    @Test
    void invalidFieldReturns400WithSingleFieldError() {
        assertThat(mvc.get().uri("/api/v1/test/invalid-field"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.detail").isEqualTo("Request validation failed");
                    assertThat(json).extractingPath("$.errors[0].field").isEqualTo("categoryCode");
                    assertThat(json).extractingPath("$.errors[0].message").isEqualTo("Unknown category");
                });
    }

    @Test
    void unexpectedErrorReturns500WithoutLeakingDetails() {
        assertThat(mvc.get().uri("/api/v1/test/unexpected"))
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .bodyText()
                .contains("An unexpected error occurred")
                .doesNotContain("users_email_key");
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Business", 42);
        }

        @GetMapping("/test/business-rule")
        void businessRule() {
            throw new BusinessRuleException("SLOT_TAKEN", "This time slot is no longer available");
        }

        @PostMapping("/test/validation")
        void validation(@Valid @RequestBody NameRequest request) {}

        @GetMapping("/test/invalid-field")
        void invalidField() {
            throw new InvalidFieldException("categoryCode", "Unknown category");
        }

        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new IllegalStateException("duplicate key value violates unique constraint \"users_email_key\"");
        }
    }

    record NameRequest(@NotBlank String name) {}
}
