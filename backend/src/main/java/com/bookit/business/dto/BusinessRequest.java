package com.bookit.business.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BusinessRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 30) String categoryCode,
        @NotBlank @Pattern(regexp = "\\+?[0-9]{9,15}") String phone,
        @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 150) String street,
        @NotBlank @Size(max = 80) String city,
        @NotBlank @Pattern(regexp = "\\d{2}-\\d{3}", message = "must match the format 00-000") String postalCode) {
}
