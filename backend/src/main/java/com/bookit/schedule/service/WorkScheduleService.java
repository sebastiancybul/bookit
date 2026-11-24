package com.bookit.schedule.service;

import com.bookit.schedule.dto.WorkScheduleRequest;
import com.bookit.schedule.dto.WorkScheduleResponse;
import java.util.UUID;

public interface WorkScheduleService {

    WorkScheduleResponse get(UUID ownerId, UUID employeeId);

    WorkScheduleResponse replace(UUID ownerId, UUID employeeId, WorkScheduleRequest request);
}
