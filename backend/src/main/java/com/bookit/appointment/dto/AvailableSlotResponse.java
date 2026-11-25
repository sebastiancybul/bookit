package com.bookit.appointment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

public record AvailableSlotResponse(
        @Schema(example = "2026-11-26T09:15:00Z")
        Instant startTime,

        @Schema(type = "string", example = "10:15")
        @JsonFormat(pattern = "HH:mm")
        LocalTime localTime) {

    public static AvailableSlotResponse of(Instant startTime, ZoneId zone) {
        return new AvailableSlotResponse(startTime, startTime.atZone(zone).toLocalTime());
    }
}
