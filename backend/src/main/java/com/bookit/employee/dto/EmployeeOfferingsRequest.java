package com.bookit.employee.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record EmployeeOfferingsRequest(
        @NotNull @Size(max = 100) Set<@NotNull UUID> offeringIds) {
}
