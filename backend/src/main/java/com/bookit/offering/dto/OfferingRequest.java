package com.bookit.offering.dto;

import com.bookit.shared.validation.MultipleOf;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OfferingRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 1000) String description,
        @NotNull @Min(0) @Max(10_000_000) Long priceAmount,
        @NotNull @Min(5) @Max(480) @MultipleOf(5) Integer durationMinutes,
        @NotNull @Min(0) @Max(120) @MultipleOf(5) Integer bufferMinutes) {
}
