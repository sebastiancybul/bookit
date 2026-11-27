package com.bookit.appointment.service;

import com.bookit.appointment.dto.AvailableSlotResponse;
import com.bookit.offering.service.OfferingService.BookableOffering;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AvailabilityService {

    List<AvailableSlotResponse> getAvailableSlots(String businessSlug, UUID offeringId, UUID employeeId, LocalDate date);

    BookableOffering requireAvailableSlot(UUID offeringId, UUID employeeId, Instant startTime);
}
