package com.bookit.appointment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.bookit.appointment.dto.AppointmentResponse;
import com.bookit.appointment.dto.CreateAppointmentRequest;
import com.bookit.appointment.entity.Appointment;
import com.bookit.appointment.entity.AppointmentStatus;
import com.bookit.appointment.repository.AppointmentRepository;
import com.bookit.appointment.service.AvailabilityService;
import com.bookit.appointment.service.BookingService.BookingResult;
import com.bookit.offering.service.OfferingService.BookableOffering;
import com.bookit.shared.error.BusinessRuleException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID KEY = UUID.randomUUID();
    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final UUID OFFERING_ID = UUID.randomUUID();
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final Instant START = Instant.parse("2026-11-27T08:00:00Z");
    private static final BookableOffering OFFERING =
            new BookableOffering(OFFERING_ID, BUSINESS_ID, "Haircut", 30, 15, 5000, "PLN");
    private static final CreateAppointmentRequest REQUEST =
            new CreateAppointmentRequest(OFFERING_ID, EMPLOYEE_ID, START, "Short on the sides");

    @Mock
    AppointmentRepository appointmentRepository;

    @Mock
    AvailabilityService availabilityService;

    @Mock
    AppointmentWriter appointmentWriter;

    BookingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BookingServiceImpl(appointmentRepository, availabilityService, appointmentWriter);
    }

    @Test
    void newBookingIsInsertedWithSnapshotAndReportedAsCreated() {
        given(appointmentRepository.findByClientIdAndIdempotencyKey(CLIENT_ID, KEY)).willReturn(Optional.empty());
        given(availabilityService.requireAvailableSlot(OFFERING_ID, EMPLOYEE_ID, START)).willReturn(OFFERING);
        given(appointmentWriter.insert(any())).willAnswer(call -> AppointmentResponse.from(call.getArgument(0)));
        ArgumentCaptor<Appointment> inserted = ArgumentCaptor.forClass(Appointment.class);

        BookingResult result = service.book(CLIENT_ID, KEY, REQUEST);

        assertThat(result.created()).isTrue();
        then(appointmentWriter).should().insert(inserted.capture());
        Appointment appointment = inserted.getValue();
        assertThat(appointment.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(appointment.getBusinessId()).isEqualTo(BUSINESS_ID);
        assertThat(appointment.getIdempotencyKey()).isEqualTo(KEY);
        assertThat(appointment.getEndTime()).isEqualTo(Instant.parse("2026-11-27T08:30:00Z"));
        assertThat(appointment.getBlockedUntil()).isEqualTo(Instant.parse("2026-11-27T08:45:00Z"));
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(appointment.getOfferingName()).isEqualTo("Haircut");
        assertThat(appointment.getPriceAmount()).isEqualTo(5000);
    }

    @Test
    void sequentialReplayReturnsExistingAppointmentWithoutCheckingOrInserting() {
        given(appointmentRepository.findByClientIdAndIdempotencyKey(CLIENT_ID, KEY))
                .willReturn(Optional.of(existing("Short on the sides")));

        BookingResult result = service.book(CLIENT_ID, KEY, REQUEST);

        assertThat(result.created()).isFalse();
        assertThat(result.appointment().startTime()).isEqualTo(START);
        then(availabilityService).shouldHaveNoInteractions();
        then(appointmentWriter).shouldHaveNoInteractions();
    }

    @Test
    void replayComparesNormalizedNote() {
        given(appointmentRepository.findByClientIdAndIdempotencyKey(CLIENT_ID, KEY))
                .willReturn(Optional.of(existing(null)));

        BookingResult result =
                service.book(CLIENT_ID, KEY, new CreateAppointmentRequest(OFFERING_ID, EMPLOYEE_ID, START, "   "));

        assertThat(result.created()).isFalse();
    }

    @Test
    void sameKeyWithDifferentPayloadReturns422() {
        given(appointmentRepository.findByClientIdAndIdempotencyKey(CLIENT_ID, KEY))
                .willReturn(Optional.of(existing("Short on the sides")));
        CreateAppointmentRequest otherTime =
                new CreateAppointmentRequest(OFFERING_ID, EMPLOYEE_ID, START.plusSeconds(3600), "Short on the sides");

        assertThatThrownBy(() -> service.book(CLIENT_ID, KEY, otherTime))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
                });
        then(appointmentWriter).shouldHaveNoInteractions();
    }

    @Test
    void unavailableSlotIsNotInserted() {
        given(appointmentRepository.findByClientIdAndIdempotencyKey(CLIENT_ID, KEY)).willReturn(Optional.empty());
        given(availabilityService.requireAvailableSlot(OFFERING_ID, EMPLOYEE_ID, START))
                .willThrow(AvailabilityServiceImpl.slotTaken());

        assertThatThrownBy(() -> service.book(CLIENT_ID, KEY, REQUEST))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("SLOT_TAKEN"));
        then(appointmentWriter).should(never()).insert(any());
    }

    @Test
    void slotTakenByOwnConcurrentDuplicateReturnsTheWinner() {
        given(appointmentRepository.findByClientIdAndIdempotencyKey(CLIENT_ID, KEY))
                .willReturn(Optional.empty(), Optional.of(existing("Short on the sides")));
        given(availabilityService.requireAvailableSlot(OFFERING_ID, EMPLOYEE_ID, START))
                .willThrow(AvailabilityServiceImpl.slotTaken());

        BookingResult result = service.book(CLIENT_ID, KEY, REQUEST);

        assertThat(result.created()).isFalse();
        then(appointmentWriter).shouldHaveNoInteractions();
    }

    @Test
    void integrityErrorCausedByOwnConcurrentDuplicateReturnsTheWinner() {
        given(appointmentRepository.findByClientIdAndIdempotencyKey(CLIENT_ID, KEY))
                .willReturn(Optional.empty(), Optional.of(existing("Short on the sides")));
        given(availabilityService.requireAvailableSlot(OFFERING_ID, EMPLOYEE_ID, START)).willReturn(OFFERING);
        given(appointmentWriter.insert(any()))
                .willThrow(violation("23P01", "ex_appointments_employee_overlap"));

        BookingResult result = service.book(CLIENT_ID, KEY, REQUEST);

        assertThat(result.created()).isFalse();
        assertThat(result.appointment().startTime()).isEqualTo(START);
    }

    @Test
    void employeeOverlapFromDatabaseReturnsSlotTaken() {
        stubInsertFailingWith(violation("23P01", BookingServiceImpl.EMPLOYEE_OVERLAP_CONSTRAINT));

        assertThatThrownBy(() -> service.book(CLIENT_ID, KEY, REQUEST))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("SLOT_TAKEN");
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    void clientOverlapFromDatabaseReturnsClientOverlap() {
        stubInsertFailingWith(violation("23P01", BookingServiceImpl.CLIENT_OVERLAP_CONSTRAINT));

        assertThatThrownBy(() -> service.book(CLIENT_ID, KEY, REQUEST))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("CLIENT_OVERLAP");
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    void otherIntegrityErrorIsRethrownUnchanged() {
        DataIntegrityViolationException foreignKey = violation("23503", "fk_appointments_offering");
        stubInsertFailingWith(foreignKey);

        assertThatThrownBy(() -> service.book(CLIENT_ID, KEY, REQUEST)).isSameAs(foreignKey);
    }

    @Test
    void deadlockWithOwnConcurrentDuplicateReturnsTheWinnerWithoutRetrying() {
        given(appointmentRepository.findByClientIdAndIdempotencyKey(CLIENT_ID, KEY))
                .willReturn(Optional.empty(), Optional.of(existing("Short on the sides")));
        given(availabilityService.requireAvailableSlot(OFFERING_ID, EMPLOYEE_ID, START)).willReturn(OFFERING);
        given(appointmentWriter.insert(any())).willThrow(deadlock());

        BookingResult result = service.book(CLIENT_ID, KEY, REQUEST);

        assertThat(result.created()).isFalse();
        then(appointmentWriter).should(times(1)).insert(any());
    }

    @Test
    void deadlockIsRetriedOnceWithFreshEntityAndThenTranslated() {
        given(appointmentRepository.findByClientIdAndIdempotencyKey(CLIENT_ID, KEY)).willReturn(Optional.empty());
        given(availabilityService.requireAvailableSlot(OFFERING_ID, EMPLOYEE_ID, START)).willReturn(OFFERING);
        given(appointmentWriter.insert(any()))
                .willThrow(deadlock())
                .willThrow(violation("23P01", BookingServiceImpl.CLIENT_OVERLAP_CONSTRAINT));
        ArgumentCaptor<Appointment> inserted = ArgumentCaptor.forClass(Appointment.class);

        assertThatThrownBy(() -> service.book(CLIENT_ID, KEY, REQUEST))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("CLIENT_OVERLAP"));
        then(appointmentWriter).should(times(2)).insert(inserted.capture());
        assertThat(inserted.getAllValues().get(1)).isNotSameAs(inserted.getAllValues().get(0));
    }

    @Test
    void deadlockRetryCanSucceed() {
        given(appointmentRepository.findByClientIdAndIdempotencyKey(CLIENT_ID, KEY)).willReturn(Optional.empty());
        given(availabilityService.requireAvailableSlot(OFFERING_ID, EMPLOYEE_ID, START)).willReturn(OFFERING);
        given(appointmentWriter.insert(any()))
                .willThrow(deadlock())
                .willAnswer(call -> AppointmentResponse.from(call.getArgument(0)));

        assertThat(service.book(CLIENT_ID, KEY, REQUEST).created()).isTrue();
    }

    @Test
    void bookRunsWithoutTransactionSoTheRetryLookupGetsAFreshOne() throws NoSuchMethodException {
        Method book = BookingServiceImpl.class.getMethod(
                "book", UUID.class, UUID.class, CreateAppointmentRequest.class);
        Method insert = AppointmentWriter.class.getMethod("insert", Appointment.class);

        assertThat(AnnotatedElementUtils.hasAnnotation(BookingServiceImpl.class, Transactional.class)).isFalse();
        assertThat(AnnotatedElementUtils.hasAnnotation(book, Transactional.class)).isFalse();
        assertThat(AnnotatedElementUtils.hasAnnotation(insert, Transactional.class)).isTrue();
    }

    private void stubInsertFailingWith(DataIntegrityViolationException ex) {
        given(appointmentRepository.findByClientIdAndIdempotencyKey(CLIENT_ID, KEY)).willReturn(Optional.empty());
        given(availabilityService.requireAvailableSlot(OFFERING_ID, EMPLOYEE_ID, START)).willReturn(OFFERING);
        given(appointmentWriter.insert(any())).willThrow(ex);
    }

    private static Appointment existing(String note) {
        return Appointment.book(CLIENT_ID, EMPLOYEE_ID, OFFERING, START, note, KEY);
    }

    private static CannotAcquireLockException deadlock() {
        return new CannotAcquireLockException(
                "could not execute statement", new SQLException("ERROR: deadlock detected", "40P01"));
    }

    private static DataIntegrityViolationException violation(String sqlState, String constraint) {
        SQLException sql = new SQLException(
                "ERROR: conflicting key value violates constraint \"" + constraint + "\"", sqlState);
        return new DataIntegrityViolationException("could not execute statement", sql);
    }
}
