package com.bookit.schedule.dto;

import com.bookit.schedule.entity.WorkingHoursInterval;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record WorkScheduleRequest(
        @NotNull @Size(max = 50) List<@NotNull @Valid WorkingHoursIntervalDto> intervals) {

    public List<WorkingHoursInterval> toIntervals() {
        return intervals.stream().map(WorkingHoursIntervalDto::toInterval).toList();
    }
}
