package com.bookit.schedule.service;

import com.bookit.schedule.dto.WorkScheduleRequest;
import com.bookit.schedule.dto.WorkScheduleResponse;
import com.bookit.schedule.entity.WorkingHoursInterval;
import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

public interface WorkScheduleService {

    WorkScheduleResponse get(UUID ownerId, UUID employeeId);

    WorkScheduleResponse replace(UUID ownerId, UUID employeeId, WorkScheduleRequest request);

    List<WorkingHoursInterval> getIntervals(UUID employeeId, DayOfWeek day);
}
