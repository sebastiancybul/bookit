package com.bookit.schedule.slot;

import com.bookit.schedule.entity.WorkingHoursInterval;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public final class SlotCalculator {

    private SlotCalculator() {
    }

    public static List<Instant> availableStarts(SlotRequest request) {
        int step = request.stepMinutes();
        Duration blocked = Duration.ofMinutes(request.durationMinutes() + request.bufferMinutes());
        List<Instant> starts = new ArrayList<>();
        for (WorkingHoursInterval interval : request.intervals()) {
            Instant intervalEnd = toInstant(request.date().atTime(interval.endTime()), request.zone());
            int lastMinute = minuteOfDay(interval.endTime());
            for (int minute = roundUpToStep(minuteOfDay(interval.startTime()), step);
                    minute < lastMinute;
                    minute += step) {
                LocalDateTime localStart = request.date().atTime(LocalTime.ofSecondOfDay(minute * 60L));
                if (!exists(localStart, request.zone())) {
                    continue;
                }
                Instant start = toInstant(localStart, request.zone());
                if (!start.plus(blocked).isAfter(intervalEnd)
                        && !start.isBefore(request.earliestStart())
                        && isFree(start, blocked, request.busyRanges())) {
                    starts.add(start);
                }
            }
        }
        return starts;
    }

    private static boolean exists(LocalDateTime localTime, ZoneId zone) {
        return !zone.getRules().getValidOffsets(localTime).isEmpty();
    }

    private static Instant toInstant(LocalDateTime localTime, ZoneId zone) {
        return localTime.atZone(zone).toInstant();
    }

    private static boolean isFree(Instant start, Duration blocked, List<BusyRange> busyRanges) {
        Instant end = start.plus(blocked);
        return busyRanges.stream().noneMatch(busy -> busy.overlaps(start, end));
    }

    private static int minuteOfDay(LocalTime time) {
        return time.toSecondOfDay() / 60;
    }

    private static int roundUpToStep(int minute, int step) {
        return (minute + step - 1) / step * step;
    }
}
