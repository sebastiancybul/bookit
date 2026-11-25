package com.bookit.schedule.slot;

import com.bookit.schedule.entity.WorkingHoursInterval;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

public record SlotRequest(
        LocalDate date,
        ZoneId zone,
        List<WorkingHoursInterval> intervals,
        int durationMinutes,
        int bufferMinutes,
        int stepMinutes,
        List<BusyRange> busyRanges,
        Instant earliestStart) {

    public SlotRequest {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(earliestStart, "earliestStart");
        intervals = List.copyOf(intervals);
        busyRanges = List.copyOf(busyRanges);
        if (durationMinutes <= 0 || bufferMinutes < 0 || stepMinutes <= 0) {
            throw new IllegalArgumentException("Duration and step must be positive, buffer must not be negative");
        }
        if (intervals.stream().anyMatch(interval -> interval.dayOfWeek() != date.getDayOfWeek())) {
            throw new IllegalArgumentException("All intervals must be on " + date.getDayOfWeek());
        }
    }
}
