package com.bookit.employee.controller;

import com.bookit.auth.security.AuthCookies;
import com.bookit.auth.security.CurrentUserId;
import com.bookit.auth.service.AuthService;
import com.bookit.employee.dto.EmployeeAccountRequest;
import com.bookit.employee.dto.EmployeeOfferingsRequest;
import com.bookit.employee.dto.EmployeeRequest;
import com.bookit.employee.dto.EmployeeResponse;
import com.bookit.employee.service.EmployeeService;
import com.bookit.employee.service.EmployeeService.AccountChange;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Owner employees")
@RestController
@RequestMapping("/owner/business/employees")
@RequiredArgsConstructor
public class OwnerEmployeeController {

    private final EmployeeService employeeService;
    private final AuthService authService;
    private final AuthCookies authCookies;

    @GetMapping(version = "1")
    public List<EmployeeResponse> getMyEmployees(@CurrentUserId UUID userId) {
        return employeeService.getMine(userId);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(version = "1")
    public EmployeeResponse createEmployee(@CurrentUserId UUID userId, @Valid @RequestBody EmployeeRequest request) {
        return employeeService.create(userId, request);
    }

    @PutMapping(path = "/{id}", version = "1")
    public EmployeeResponse updateEmployee(
            @CurrentUserId UUID userId, @PathVariable UUID id, @Valid @RequestBody EmployeeRequest request) {
        return employeeService.update(userId, id, request);
    }

    @PutMapping(path = "/{id}/offerings", version = "1")
    public EmployeeResponse assignEmployeeOfferings(
            @CurrentUserId UUID userId, @PathVariable UUID id, @Valid @RequestBody EmployeeOfferingsRequest request) {
        return employeeService.assignOfferings(userId, id, request.offeringIds());
    }

    @PutMapping(path = "/{id}/account", version = "1")
    public ResponseEntity<EmployeeResponse> linkEmployeeAccount(
            @CurrentUserId UUID userId, @PathVariable UUID id, @Valid @RequestBody EmployeeAccountRequest request) {
        AccountChange change = employeeService.linkAccount(userId, id, request.email());
        return ResponseEntity.ok().headers(ownTokenCookie(userId, change)).body(change.employee());
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(path = "/{id}/account", version = "1")
    public ResponseEntity<Void> unlinkEmployeeAccount(@CurrentUserId UUID userId, @PathVariable UUID id) {
        AccountChange change = employeeService.unlinkAccount(userId, id);
        return ResponseEntity.noContent().headers(ownTokenCookie(userId, change)).build();
    }

    @ApiResponse(responseCode = "204", description = "Employee deactivated")
    @ApiResponse(
            responseCode = "409",
            description = "code EMPLOYEE_HAS_UPCOMING_APPOINTMENTS: the employee has upcoming or ongoing "
                    + "confirmed appointments; cancel them first",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(path = "/{id}", version = "1")
    public void deleteEmployee(@CurrentUserId UUID userId, @PathVariable UUID id) {
        employeeService.delete(userId, id);
    }

    private HttpHeaders ownTokenCookie(UUID userId, AccountChange change) {
        HttpHeaders headers = new HttpHeaders();
        if (change.ownRolesChanged()) {
            headers.add(HttpHeaders.SET_COOKIE, authCookies.accessToken(authService.reissueToken(userId)).toString());
        }
        return headers;
    }
}
