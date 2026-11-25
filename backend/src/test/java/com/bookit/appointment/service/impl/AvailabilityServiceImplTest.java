package com.bookit.appointment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.bookit.appointment.config.BookingProperties;
import com.bookit.appointment.dto.AvailableSlotResponse;
import com.bookit.business.service.BusinessService;
import com.bookit.business.service.BusinessService.BookingSettings;
import com.bookit.employee.service.EmployeeService;
import com.bookit.offering.service.OfferingService;
import com.bookit.offering.service.OfferingService.BookableOffering;
import com.bookit.schedule.entity.WorkingHoursInterval;
import com.bookit.schedule.service.WorkScheduleService;
import com.bookit.shared.error.InvalidFieldException;
import com.bookit.shared.error.ResourceNotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceImplTest {

    private static final String SLUG = "k3f9x2ab-barber-krakow";
    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final UUID OFFERING_ID = UUID.randomUUID();
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final Instant NOW = Instant.parse("2026-11-26T09:20:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 26);

    @Mock
    BusinessService businessService;

    @Mock
    OfferingService offeringService;

    @Mock
    EmployeeService employeeService;

    @Mock
    WorkScheduleService workScheduleService;

    AvailabilityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = serviceAt(NOW);
    }

    @Test
    void returnsSlotsWithStartInstantAndLocalTimeOfBusiness() {
        LocalDate tomorrow = TODAY.plusDays(1);
        stubBookableOffering(BUSINESS_ID);
        given(workScheduleService.getIntervals(EMPLOYEE_ID, tomorrow.getDayOfWeek()))
                .willReturn(List.of(interval(tomorrow, "09:00", "10:00")));

        List<AvailableSlotResponse> slots = service.getAvailableSlots(SLUG, OFFERING_ID, EMPLOYEE_ID, tomorrow);

        assertThat(slots).containsExactly(
                new AvailableSlotResponse(Instant.parse("2026-11-27T08:00:00Z"), LocalTime.of(9, 0)),
                new AvailableSlotResponse(Instant.parse("2026-11-27T08:15:00Z"), LocalTime.of(9, 15)),
                new AvailableSlotResponse(Instant.parse("2026-11-27T08:30:00Z"), LocalTime.of(9, 30)));
        then(employeeService).should().requireBookable(EMPLOYEE_ID, BUSINESS_ID, OFFERING_ID);
    }

    @Test
    void todayStartsNoEarlierThanNowPlusMinLeadTime() {
        stubBookableOffering(BUSINESS_ID);
        given(workScheduleService.getIntervals(EMPLOYEE_ID, TODAY.getDayOfWeek()))
                .willReturn(List.of(interval(TODAY, "09:00", "12:00")));

        List<AvailableSlotResponse> slots = service.getAvailableSlots(SLUG, OFFERING_ID, EMPLOYEE_ID, TODAY);

        assertThat(slots).extracting(AvailableSlotResponse::localTime)
                .containsExactly(LocalTime.of(11, 30));
    }

    @Test
    void rejectsDateInThePast() {
        stubBookableOffering(BUSINESS_ID);

        assertThatThrownBy(() -> service.getAvailableSlots(SLUG, OFFERING_ID, EMPLOYEE_ID, TODAY.minusDays(1)))
                .isInstanceOf(InvalidFieldException.class)
                .hasFieldOrPropertyWithValue("field", "date");
    }

    @Test
    void acceptsLastDayOfBookingWindow() {
        stubBookableOffering(BUSINESS_ID);

        List<AvailableSlotResponse> slots =
                service.getAvailableSlots(SLUG, OFFERING_ID, EMPLOYEE_ID, TODAY.plusDays(180));

        assertThat(slots).isEmpty();
    }

    @Test
    void rejectsDateBeyondBookingWindow() {
        stubBookableOffering(BUSINESS_ID);

        assertThatThrownBy(() -> service.getAvailableSlots(SLUG, OFFERING_ID, EMPLOYEE_ID, TODAY.plusDays(181)))
                .isInstanceOf(InvalidFieldException.class)
                .hasFieldOrPropertyWithValue("field", "date");
    }

    @Test
    void decidesTodayInBusinessTimeZoneNotInUtc() {
        service = serviceAt(Instant.parse("2026-11-26T23:30:00Z"));
        stubBookableOffering(BUSINESS_ID);

        assertThatThrownBy(() -> service.getAvailableSlots(SLUG, OFFERING_ID, EMPLOYEE_ID, TODAY))
                .isInstanceOf(InvalidFieldException.class);
        assertThat(service.getAvailableSlots(SLUG, OFFERING_ID, EMPLOYEE_ID, TODAY.plusDays(1))).isEmpty();
    }

    @Test
    void offeringOfAnotherBusinessIsNotFound() {
        stubBookableOffering(UUID.randomUUID());

        assertThatThrownBy(() -> service.getAvailableSlots(SLUG, OFFERING_ID, EMPLOYEE_ID, TODAY))
                .isInstanceOf(ResourceNotFoundException.class);
        then(employeeService).should(never()).requireBookable(any(), any(), any());
    }

    private AvailabilityServiceImpl serviceAt(Instant now) {
        return new AvailabilityServiceImpl(
                businessService,
                offeringService,
                employeeService,
                workScheduleService,
                new BookingProperties(Duration.ofHours(1), 180),
                Clock.fixed(now, ZoneOffset.UTC));
    }

    private void stubBookableOffering(UUID offeringBusinessId) {
        given(businessService.getPublicBusinessId(SLUG)).willReturn(BUSINESS_ID);
        given(businessService.getBookingSettings(BUSINESS_ID)).willReturn(new BookingSettings(BUSINESS_ID, WARSAW, 15));
        given(offeringService.getBookable(OFFERING_ID)).willReturn(
                new BookableOffering(OFFERING_ID, offeringBusinessId, "Haircut", 30, 0, 5000, "PLN"));
    }

    private static WorkingHoursInterval interval(LocalDate date, String start, String end) {
        return new WorkingHoursInterval(date.getDayOfWeek(), LocalTime.parse(start), LocalTime.parse(end));
    }
}
