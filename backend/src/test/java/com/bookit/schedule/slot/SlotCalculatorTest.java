package com.bookit.schedule.slot;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookit.schedule.entity.WorkingHoursInterval;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class SlotCalculatorTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final LocalDate THURSDAY = LocalDate.of(2026, 11, 26);
    private static final LocalDate SPRING_FORWARD = LocalDate.of(2027, 3, 28);
    private static final LocalDate FALL_BACK = LocalDate.of(2026, 10, 25);
    private static final Instant LONG_AGO = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void startsOnGridWhileServiceFitsInInterval() {
        SlotRequest request = request(List.of(interval("09:00", "10:00")), 30, 0, 15);

        List<Instant> starts = SlotCalculator.availableStarts(request);

        assertThat(localTimes(starts)).containsExactly(time("09:00"), time("09:15"), time("09:30"));
    }

    @Test
    void alignsStartsToGridFromMidnightNotFromIntervalStart() {
        SlotRequest request = request(List.of(interval("09:05", "10:00")), 30, 0, 15);

        List<Instant> starts = SlotCalculator.availableStarts(request);

        assertThat(localTimes(starts)).containsExactly(time("09:15"), time("09:30"));
    }

    @Test
    void bufferMustAlsoFitInInterval() {
        SlotRequest request = request(List.of(interval("09:00", "10:00")), 30, 15, 15);

        List<Instant> starts = SlotCalculator.availableStarts(request);

        assertThat(localTimes(starts)).containsExactly(time("09:00"), time("09:15"));
    }

    @Test
    void neverSpansBreakBetweenIntervals() {
        SlotRequest request = request(List.of(interval("09:00", "10:00"), interval("11:00", "12:00")), 30, 0, 15);

        List<Instant> starts = SlotCalculator.availableStarts(request);

        assertThat(localTimes(starts)).containsExactly(
                time("09:00"), time("09:15"), time("09:30"),
                time("11:00"), time("11:15"), time("11:30"));
    }

    @Test
    void skipsStartsBeforeEarliestStartButKeepsStartExactlyOnIt() {
        SlotRequest request = request(List.of(interval("09:00", "11:00")), 30, 0, 15, at("09:30"));

        List<Instant> starts = SlotCalculator.availableStarts(request);

        assertThat(localTimes(starts)).containsExactly(
                time("09:30"), time("09:45"), time("10:00"), time("10:15"), time("10:30"));
    }

    @Test
    void skipsStartsOverlappingBusyRangeButAllowsTouchingIt() {
        SlotRequest request = request(
                List.of(interval("09:00", "11:00")), 30, 0, 15, LONG_AGO, List.of(busy("09:30", "10:15")));

        List<Instant> starts = SlotCalculator.availableStarts(request);

        assertThat(localTimes(starts)).containsExactly(time("09:00"), time("10:15"), time("10:30"));
    }

    @Test
    void bufferMustNotOverlapNextBusyRange() {
        SlotRequest request = request(
                List.of(interval("09:00", "11:30")), 30, 15, 15, LONG_AGO, List.of(busy("10:00", "10:30")));

        List<Instant> starts = SlotCalculator.availableStarts(request);

        assertThat(localTimes(starts)).containsExactly(time("09:00"), time("09:15"), time("10:30"), time("10:45"));
    }

    @Test
    void handlesIntervalEndingJustBeforeMidnight() {
        SlotRequest request = request(List.of(interval("23:00", "23:55")), 30, 0, 15);

        List<Instant> starts = SlotCalculator.availableStarts(request);

        assertThat(localTimes(starts)).containsExactly(time("23:00"), time("23:15"));
    }

    @Test
    void skipsLocalTimesThatDoNotExistWhenClocksSpringForward() {
        SlotRequest request = new SlotRequest(
                SPRING_FORWARD, WARSAW, List.of(interval(SPRING_FORWARD, "01:00", "04:00")),
                30, 0, 30, List.of(), LONG_AGO);

        List<Instant> starts = SlotCalculator.availableStarts(request);

        assertThat(localTimes(starts)).containsExactly(time("01:00"), time("01:30"), time("03:00"), time("03:30"));
    }

    @Test
    void measuresFitInRealMinutesWhenClocksSpringForward() {
        SlotRequest request = new SlotRequest(
                SPRING_FORWARD, WARSAW, List.of(interval(SPRING_FORWARD, "01:00", "03:00")),
                60, 0, 30, List.of(), LONG_AGO);

        List<Instant> starts = SlotCalculator.availableStarts(request);

        assertThat(localTimes(starts)).containsExactly(time("01:00"));
    }

    @Test
    void measuresFitInRealMinutesAndTakesEarlierOffsetWhenClocksFallBack() {
        SlotRequest request = new SlotRequest(
                FALL_BACK, WARSAW, List.of(interval(FALL_BACK, "01:00", "03:00")),
                90, 0, 30, List.of(), LONG_AGO);

        List<Instant> starts = SlotCalculator.availableStarts(request);

        assertThat(starts).containsExactly(
                Instant.parse("2026-10-24T23:00:00Z"),
                Instant.parse("2026-10-24T23:30:00Z"),
                Instant.parse("2026-10-25T00:00:00Z"),
                Instant.parse("2026-10-25T00:30:00Z"));
    }

    private static SlotRequest request(
            List<WorkingHoursInterval> intervals, int durationMinutes, int bufferMinutes, int stepMinutes) {
        return request(intervals, durationMinutes, bufferMinutes, stepMinutes, LONG_AGO);
    }

    private static SlotRequest request(
            List<WorkingHoursInterval> intervals,
            int durationMinutes,
            int bufferMinutes,
            int stepMinutes,
            Instant earliestStart) {
        return request(intervals, durationMinutes, bufferMinutes, stepMinutes, earliestStart, List.of());
    }

    private static SlotRequest request(
            List<WorkingHoursInterval> intervals,
            int durationMinutes,
            int bufferMinutes,
            int stepMinutes,
            Instant earliestStart,
            List<BusyRange> busyRanges) {
        return new SlotRequest(
                THURSDAY, WARSAW, intervals, durationMinutes, bufferMinutes, stepMinutes, busyRanges, earliestStart);
    }

    private static BusyRange busy(String start, String end) {
        return new BusyRange(at(start), at(end));
    }

    private static Instant at(String localTime) {
        return THURSDAY.atTime(time(localTime)).atZone(WARSAW).toInstant();
    }

    private static WorkingHoursInterval interval(String start, String end) {
        return interval(THURSDAY, start, end);
    }

    private static WorkingHoursInterval interval(LocalDate date, String start, String end) {
        return new WorkingHoursInterval(date.getDayOfWeek(), time(start), time(end));
    }

    private static LocalTime time(String value) {
        return LocalTime.parse(value);
    }

    private static List<LocalTime> localTimes(List<Instant> starts) {
        return starts.stream()
                .map(start -> start.atZone(WARSAW).toLocalTime())
                .toList();
    }
}
