package com.bookit.schedule.slot;

import java.time.Instant;
import java.util.Objects;

public record BusyRange(Instant start, Instant end) {

    public BusyRange {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Busy range start must be before end");
        }
    }

    public boolean overlaps(Instant otherStart, Instant otherEnd) {
        return start.isBefore(otherEnd) && otherStart.isBefore(end);
    }
}
