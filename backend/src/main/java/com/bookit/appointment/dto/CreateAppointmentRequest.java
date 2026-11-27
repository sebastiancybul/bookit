package com.bookit.appointment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record CreateAppointmentRequest(
        @NotNull UUID offeringId,
        @NotNull UUID employeeId,
        @NotNull @Schema(example = "2026-12-05T13:00:00Z") Instant startTime,
        @Size(max = 500) @Schema(types = {"string", "null"}) String clientNote) {
}
