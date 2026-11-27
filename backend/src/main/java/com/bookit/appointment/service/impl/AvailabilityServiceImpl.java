package com.bookit.appointment.service.impl;

import com.bookit.appointment.config.BookingProperties;
import com.bookit.appointment.dto.AvailableSlotResponse;
import com.bookit.appointment.repository.AppointmentRepository;
import com.bookit.appointment.service.AvailabilityService;
import com.bookit.business.service.BusinessService;
import com.bookit.business.service.BusinessService.BookingSettings;
import com.bookit.employee.service.EmployeeService;
import com.bookit.offering.service.OfferingService;
import com.bookit.offering.service.OfferingService.BookableOffering;
import com.bookit.schedule.service.WorkScheduleService;
import com.bookit.schedule.slot.SlotCalculator;
import com.bookit.schedule.slot.SlotRequest;
import com.bookit.shared.error.BusinessRuleException;
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
    private final AppointmentRepository appointmentRepository;
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
        LocalDate today = now.atZone(settings.zone()).toLocalDate();
        if (date.isBefore(today)) {
            throw new InvalidFieldException(DATE_FIELD, "Date must not be in the past");
        }
        if (!isWithinBookingWindow(date, today)) {
            throw new InvalidFieldException(
                    DATE_FIELD, "Date must be at most " + bookingProperties.maxDaysAhead() + " days ahead");
        }
        return availableStarts(settings, offering, employeeId, date, now).stream()
                .map(start -> AvailableSlotResponse.of(start, settings.zone()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookableOffering requireAvailableSlot(UUID offeringId, UUID employeeId, Instant startTime) {
        BookableOffering offering = offeringService.getBookable(offeringId);
        BookingSettings settings = businessService.getBookingSettings(offering.businessId());
        employeeService.requireBookable(employeeId, settings.businessId(), offeringId);

        Instant now = clock.instant();
        LocalDate today = now.atZone(settings.zone()).toLocalDate();
        LocalDate date = startTime.atZone(settings.zone()).toLocalDate();
        if (!isWithinBookingWindow(date, today)
                || !availableStarts(settings, offering, employeeId, date, now).contains(startTime)) {
            throw slotTaken();
        }
        return offering;
    }

    static BusinessRuleException slotTaken() {
        return new BusinessRuleException("SLOT_TAKEN", "This time is no longer available");
    }

    private boolean isWithinBookingWindow(LocalDate date, LocalDate today) {
        return !date.isBefore(today) && !date.isAfter(today.plusDays(bookingProperties.maxDaysAhead()));
    }

    private List<Instant> availableStarts(
            BookingSettings settings, BookableOffering offering, UUID employeeId, LocalDate date, Instant now) {
        Instant dayStart = date.atStartOfDay(settings.zone()).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(settings.zone()).toInstant();
        SlotRequest request = new SlotRequest(
                date,
                settings.zone(),
                workScheduleService.getIntervals(employeeId, date.getDayOfWeek()),
                offering.durationMinutes(),
                offering.bufferMinutes(),
                settings.slotIntervalMinutes(),
                appointmentRepository.findBusyRanges(employeeId, dayStart, dayEnd),
                now.plus(bookingProperties.minLeadTime()));
        return SlotCalculator.availableStarts(request);
    }
}
