package com.bookit.employee.controller;

import com.bookit.auth.security.CurrentUserId;
import com.bookit.employee.dto.EmployeeOfferingsRequest;
import com.bookit.employee.dto.EmployeeRequest;
import com.bookit.employee.dto.EmployeeResponse;
import com.bookit.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(path = "/{id}", version = "1")
    public void deleteEmployee(@CurrentUserId UUID userId, @PathVariable UUID id) {
        employeeService.delete(userId, id);
    }
}
