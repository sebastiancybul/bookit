package com.bookit.appointment.service.impl;

import com.bookit.appointment.config.BookingProperties;
import com.bookit.appointment.dto.AvailableSlotResponse;
import com.bookit.appointment.service.AvailabilityService;
import com.bookit.business.service.BusinessService;
import com.bookit.business.service.BusinessService.BookingSettings;
import com.bookit.employee.service.EmployeeService;
import com.bookit.offering.service.OfferingService;
import com.bookit.offering.service.OfferingService.BookableOffering;
import com.bookit.schedule.service.WorkScheduleService;
import com.bookit.schedule.slot.SlotCalculator;
import com.bookit.schedule.slot.SlotRequest;
import com.bookit.shared.error.InvalidFieldException;
import com.bookit.shared.error.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AvailabilityServiceImpl implements AvailabilityService {

    private static final String DATE_FIELD = "date";

    private final BusinessService businessService;
    private final OfferingService offeringService;
    private final EmployeeService employeeService;
    private final WorkScheduleService workScheduleService;
    private final BookingProperties bookingProperties;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> getAvailableSlots(
            String businessSlug, UUID offeringId, UUID employeeId, LocalDate date) {
        BookingSettings settings = businessService.getBookingSettings(businessService.getPublicBusinessId(businessSlug));
        BookableOffering offering = offeringService.getBookable(offeringId);
        if (!offering.businessId().equals(settings.businessId())) {
            throw new ResourceNotFoundException("Offering", offeringId);
        }
        employeeService.requireBookable(employeeId, settings.businessId(), offeringId);

        Instant now = clock.instant();
        requireWithinBookingWindow(date, now.atZone(settings.zone()).toLocalDate());

        SlotRequest request = new SlotRequest(
                date,
                settings.zone(),
                workScheduleService.getIntervals(employeeId, date.getDayOfWeek()),
                offering.durationMinutes(),
                offering.bufferMinutes(),
                settings.slotIntervalMinutes(),
                List.of(),
                now.plus(bookingProperties.minLeadTime()));
        return SlotCalculator.availableStarts(request).stream()
                .map(start -> AvailableSlotResponse.of(start, settings.zone()))
                .toList();
    }

    private void requireWithinBookingWindow(LocalDate date, LocalDate today) {
        if (date.isBefore(today)) {
            throw new InvalidFieldException(DATE_FIELD, "Date must not be in the past");
        }
        int maxDaysAhead = bookingProperties.maxDaysAhead();
        if (date.isAfter(today.plusDays(maxDaysAhead))) {
            throw new InvalidFieldException(DATE_FIELD, "Date must be at most " + maxDaysAhead + " days ahead");
        }
    }
}
