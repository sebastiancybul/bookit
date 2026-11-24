package com.bookit.schedule.entity;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.TUESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookit.shared.error.InvalidFieldException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WorkScheduleTest {

    private static final UUID EMPLOYEE_ID = UUID.randomUUID();

    @Test
    void replaceWeekStoresIntervalsSortedByDayAndStart() {
        WorkSchedule schedule = new WorkSchedule(EMPLOYEE_ID);

        schedule.replaceWeek(List.of(
                interval(TUESDAY, "09:00", "17:00"),
                interval(MONDAY, "14:00", "18:00"),
                interval(MONDAY, "08:00", "12:00")));

        assertThat(schedule.getIntervals()).containsExactly(
                interval(MONDAY, "08:00", "12:00"),
                interval(MONDAY, "14:00", "18:00"),
                interval(TUESDAY, "09:00", "17:00"));
    }

    @Test
    void replaceWeekReplacesPreviousWeek() {
        WorkSchedule schedule = new WorkSchedule(EMPLOYEE_ID);
        schedule.replaceWeek(List.of(interval(MONDAY, "09:00", "17:00")));

        schedule.replaceWeek(List.of(interval(FRIDAY, "10:00", "14:00")));

        assertThat(schedule.getIntervals()).containsExactly(interval(FRIDAY, "10:00", "14:00"));
    }

    @Test
    void emptyListClearsSchedule() {
        WorkSchedule schedule = new WorkSchedule(EMPLOYEE_ID);
        schedule.replaceWeek(List.of(interval(MONDAY, "09:00", "17:00")));

        schedule.replaceWeek(List.of());

        assertThat(schedule.getIntervals()).isEmpty();
    }

    @Test
    void touchingIntervalsAndSameHoursOnDifferentDaysAreAllowed() {
        WorkSchedule schedule = new WorkSchedule(EMPLOYEE_ID);

        schedule.replaceWeek(List.of(
                interval(MONDAY, "09:00", "12:00"),
                interval(MONDAY, "12:00", "15:00"),
                interval(TUESDAY, "09:00", "12:00")));

        assertThat(schedule.getIntervals()).hasSize(3);
    }

    @ParameterizedTest
    @CsvSource({
            "09:00, 12:00, 11:00, 14:00",
            "09:00, 17:00, 10:00, 11:00",
            "09:00, 12:00, 09:00, 10:00",
            "09:00, 12:00, 09:00, 12:00"
    })
    void overlappingIntervalsOnSameDayAreRejectedAndPreviousWeekIsKept(
            String firstStart, String firstEnd, String secondStart, String secondEnd) {
        WorkSchedule schedule = new WorkSchedule(EMPLOYEE_ID);
        schedule.replaceWeek(List.of(interval(FRIDAY, "10:00", "14:00")));

        assertThatThrownBy(() -> schedule.replaceWeek(List.of(
                        interval(MONDAY, firstStart, firstEnd),
                        interval(MONDAY, secondStart, secondEnd))))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessage("Working hours overlap on Monday")
                .extracting("field")
                .isEqualTo("intervals");

        assertThat(schedule.getIntervals()).containsExactly(interval(FRIDAY, "10:00", "14:00"));
    }

    @Test
    void nullListOrNullElementIsRejected() {
        WorkSchedule schedule = new WorkSchedule(EMPLOYEE_ID);

        assertThatThrownBy(() -> schedule.replaceWeek(null))
                .isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> schedule.replaceWeek(Arrays.asList(interval(MONDAY, "09:00", "12:00"), null)))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void intervalsCannotBeModifiedFromOutside() {
        WorkSchedule schedule = new WorkSchedule(EMPLOYEE_ID);

        assertThatThrownBy(() -> schedule.getIntervals().add(interval(MONDAY, "09:00", "12:00")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @CsvSource({"12:00, 12:00", "12:00, 09:00"})
    void intervalStartMustBeBeforeEnd(String start, String end) {
        assertThatThrownBy(() -> interval(MONDAY, start, end))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessage("Start time must be before end time on Monday");
    }

    @ParameterizedTest
    @CsvSource({"09:07, 12:00", "09:00, 12:03", "09:00:30, 12:00"})
    void intervalTimesMustBeInFiveMinuteSteps(String start, String end) {
        assertThatThrownBy(() -> interval(MONDAY, start, end))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessage("Times must be in 5-minute steps");
    }

    @Test
    void intervalRequiresAllFields() {
        assertThatThrownBy(() -> new WorkingHoursInterval(null, LocalTime.of(9, 0), LocalTime.of(12, 0)))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessage("Day, start time and end time are required");
    }

    private static WorkingHoursInterval interval(DayOfWeek day, String start, String end) {
        return new WorkingHoursInterval(day, LocalTime.parse(start), LocalTime.parse(end));
    }
}
