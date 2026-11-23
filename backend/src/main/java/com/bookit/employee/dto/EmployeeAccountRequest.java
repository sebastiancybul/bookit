package com.bookit.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmployeeAccountRequest(
        @NotBlank @Email @Size(max = 255) String email) {
}
