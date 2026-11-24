package com.bookit.schedule.controller;

import com.bookit.auth.security.CurrentUserId;
import com.bookit.schedule.dto.WorkScheduleRequest;
import com.bookit.schedule.dto.WorkScheduleResponse;
import com.bookit.schedule.service.WorkScheduleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Owner work schedules")
@RestController
@RequestMapping("/owner/business/employees/{employeeId}/schedule")
@RequiredArgsConstructor
public class OwnerWorkScheduleController {

    private final WorkScheduleService workScheduleService;

    @GetMapping(version = "1")
    public WorkScheduleResponse getWorkSchedule(@CurrentUserId UUID userId, @PathVariable UUID employeeId) {
        return workScheduleService.get(userId, employeeId);
    }

    @PutMapping(version = "1")
    public WorkScheduleResponse replaceWorkSchedule(
            @CurrentUserId UUID userId,
            @PathVariable UUID employeeId,
            @Valid @RequestBody WorkScheduleRequest request) {
        return workScheduleService.replace(userId, employeeId, request);
    }
}
