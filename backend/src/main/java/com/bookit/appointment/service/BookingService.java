package com.bookit.appointment.service;

import com.bookit.appointment.dto.AppointmentResponse;
import com.bookit.appointment.dto.CreateAppointmentRequest;
import java.util.UUID;

public interface BookingService {

    BookingResult book(UUID clientId, UUID idempotencyKey, CreateAppointmentRequest request);

    record BookingResult(
            AppointmentResponse appointment,
            boolean created) {
    }
}
