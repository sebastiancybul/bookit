package com.bookit.schedule.dto;

import com.bookit.schedule.entity.WorkSchedule;
import java.util.List;

public record WorkScheduleResponse(List<WorkingHoursIntervalDto> intervals) {

    public static WorkScheduleResponse from(WorkSchedule schedule) {
        return new WorkScheduleResponse(schedule.getIntervals().stream()
                .map(WorkingHoursIntervalDto::from)
                .toList());
    }

    public static WorkScheduleResponse empty() {
        return new WorkScheduleResponse(List.of());
    }
}
