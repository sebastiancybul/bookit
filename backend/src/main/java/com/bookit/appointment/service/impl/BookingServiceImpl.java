package com.bookit.appointment.service.impl;

import com.bookit.appointment.dto.AppointmentResponse;
import com.bookit.appointment.dto.CreateAppointmentRequest;
import com.bookit.appointment.entity.Appointment;
import com.bookit.appointment.repository.AppointmentRepository;
import com.bookit.appointment.service.AvailabilityService;
import com.bookit.appointment.service.BookingService;
import com.bookit.offering.service.OfferingService.BookableOffering;
import com.bookit.shared.error.BusinessRuleException;
import com.bookit.shared.error.PgErrors;
import com.bookit.shared.error.PgErrors.Violation;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    static final String EMPLOYEE_OVERLAP_CONSTRAINT = "ex_appointments_employee_overlap";
    static final String CLIENT_OVERLAP_CONSTRAINT = "ex_appointments_client_overlap";

    private final AppointmentRepository appointmentRepository;
    private final AvailabilityService availabilityService;
    private final AppointmentWriter appointmentWriter;

    @Override
    public BookingResult book(UUID clientId, UUID idempotencyKey, CreateAppointmentRequest request) {
        Optional<BookingResult> replay = findReplay(clientId, idempotencyKey, request);
        if (replay.isPresent()) {
            return replay.get();
        }

        BookableOffering offering = availabilityService.requireAvailableSlot(
                request.offeringId(), request.employeeId(), request.startTime());

        try {
            return insert(clientId, idempotencyKey, request, offering);
        } catch (PessimisticLockingFailureException deadlock) {
            return findReplay(clientId, idempotencyKey, request)
                    .orElseGet(() -> insert(clientId, idempotencyKey, request, offering));
        }
    }

    private BookingResult insert(
            UUID clientId, UUID idempotencyKey, CreateAppointmentRequest request, BookableOffering offering) {
        Appointment appointment = Appointment.book(
                clientId, request.employeeId(), offering, request.startTime(), request.clientNote(), idempotencyKey);
        try {
            return new BookingResult(appointmentWriter.insert(appointment), true);
        } catch (DataIntegrityViolationException ex) {
            return findReplay(clientId, idempotencyKey, request).orElseThrow(() -> translate(ex));
        }
    }

    private Optional<BookingResult> findReplay(UUID clientId, UUID idempotencyKey, CreateAppointmentRequest request) {
        return appointmentRepository.findByClientIdAndIdempotencyKey(clientId, idempotencyKey)
                .map(existing -> {
                    if (!samePayload(existing, request)) {
                        throw new BusinessRuleException(
                                HttpStatus.UNPROCESSABLE_CONTENT,
                                "IDEMPOTENCY_KEY_REUSED",
                                "This Idempotency-Key was already used for a different booking");
                    }
                    return new BookingResult(AppointmentResponse.from(existing), false);
                });
    }

    private static boolean samePayload(Appointment existing, CreateAppointmentRequest request) {
        return existing.getOfferingId().equals(request.offeringId())
                && existing.getEmployeeId().equals(request.employeeId())
                && existing.getStartTime().equals(request.startTime())
                && Objects.equals(existing.getClientNote(), Appointment.normalizeNote(request.clientNote()));
    }

    private static RuntimeException translate(DataIntegrityViolationException ex) {
        Optional<Violation> violation = PgErrors.classify(ex);
        if (violation.filter(v -> v.is(PgErrors.EXCLUSION_VIOLATION, EMPLOYEE_OVERLAP_CONSTRAINT)).isPresent()) {
            return AvailabilityServiceImpl.slotTaken();
        }
        if (violation.filter(v -> v.is(PgErrors.EXCLUSION_VIOLATION, CLIENT_OVERLAP_CONSTRAINT)).isPresent()) {
            return new BusinessRuleException("CLIENT_OVERLAP", "You already have another appointment at this time");
        }
        return ex;
    }
}
