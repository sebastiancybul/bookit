package com.bookit.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank
        @Size(min = 10, max = 72)
        @Pattern(regexp = "(?=.*\\d)(?=.*[^A-Za-z0-9]).*", message = "must contain at least one digit and one special character")
        String password,
        @NotBlank @Size(max = 50) String firstName,
        @NotBlank @Size(max = 50) String lastName,
        @NotBlank @Pattern(regexp = "\\+?[0-9]{9,15}") String phone) {
}
