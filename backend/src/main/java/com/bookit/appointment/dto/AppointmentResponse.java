package com.bookit.appointment.dto;

import com.bookit.appointment.entity.Appointment;
import com.bookit.appointment.entity.AppointmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID offeringId,
        String offeringName,
        UUID employeeId,
        @Schema(example = "2026-12-05T13:00:00Z") Instant startTime,
        @Schema(example = "2026-12-05T13:30:00Z") Instant endTime,
        AppointmentStatus status,
        long priceAmount,
        String currency,
        @Schema(types = {"string", "null"}) String clientNote) {

    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getOfferingId(),
                appointment.getOfferingName(),
                appointment.getEmployeeId(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus(),
                appointment.getPriceAmount(),
                appointment.getCurrency(),
                appointment.getClientNote());
    }
}
