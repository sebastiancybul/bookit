package com.bookit.schedule.entity;

import com.bookit.shared.error.InvalidFieldException;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.Locale;

@Embeddable
public record WorkingHoursInterval(
        @Convert(converter = DayOfWeekConverter.class)
        @Column(name = "day_of_week", nullable = false)
        DayOfWeek dayOfWeek,

        @Column(name = "start_time", nullable = false)
        LocalTime startTime,

        @Column(name = "end_time", nullable = false)
        LocalTime endTime) {

    public static final String FIELD = "intervals";
    public static final int STEP_MINUTES = 5;
    public static final Comparator<WorkingHoursInterval> CHRONOLOGICAL = Comparator
            .comparing(WorkingHoursInterval::dayOfWeek)
            .thenComparing(WorkingHoursInterval::startTime);

    public WorkingHoursInterval {
        if (dayOfWeek == null || startTime == null || endTime == null) {
            throw new InvalidFieldException(FIELD, "Day, start time and end time are required");
        }
        if (!onStep(startTime) || !onStep(endTime)) {
            throw new InvalidFieldException(FIELD, "Times must be in " + STEP_MINUTES + "-minute steps");
        }
        if (!startTime.isBefore(endTime)) {
            throw new InvalidFieldException(FIELD, "Start time must be before end time on " + dayName(dayOfWeek));
        }
    }

    public boolean overlaps(WorkingHoursInterval other) {
        return dayOfWeek == other.dayOfWeek
                && startTime.isBefore(other.endTime)
                && other.startTime.isBefore(endTime);
    }

    public static String dayName(DayOfWeek dayOfWeek) {
        return dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private static boolean onStep(LocalTime time) {
        return time.getNano() == 0 && time.toSecondOfDay() % (STEP_MINUTES * 60) == 0;
    }
}
