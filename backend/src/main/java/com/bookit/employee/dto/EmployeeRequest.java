package com.bookit.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmployeeRequest(
        @NotBlank @Size(max = 80) String displayName,
        @Size(max = 80) String position) {
}
