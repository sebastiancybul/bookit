package com.bookit.schedule.dto;

import com.bookit.schedule.entity.WorkingHoursInterval;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record WorkingHoursIntervalDto(
        @Schema(description = "ISO day of week, Monday = 1", example = "1")
        @NotNull @Min(1) @Max(7) Integer dayOfWeek,

        @Schema(type = "string", pattern = TIME_PATTERN, example = "09:00")
        @JsonFormat(pattern = "HH:mm")
        @NotNull LocalTime startTime,

        @Schema(type = "string", pattern = TIME_PATTERN, example = "17:00")
        @JsonFormat(pattern = "HH:mm")
        @NotNull LocalTime endTime) {

    static final String TIME_PATTERN = "^([01]\\d|2[0-3]):[0-5]\\d$";

    public static WorkingHoursIntervalDto from(WorkingHoursInterval interval) {
        return new WorkingHoursIntervalDto(
                interval.dayOfWeek().getValue(), interval.startTime(), interval.endTime());
    }

    public WorkingHoursInterval toInterval() {
        return new WorkingHoursInterval(DayOfWeek.of(dayOfWeek), startTime, endTime);
    }
}
