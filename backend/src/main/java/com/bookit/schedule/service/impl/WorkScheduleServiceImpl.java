package com.bookit.schedule.service.impl;

import com.bookit.employee.service.EmployeeService;
import com.bookit.schedule.dto.WorkScheduleRequest;
import com.bookit.schedule.dto.WorkScheduleResponse;
import com.bookit.schedule.entity.WorkSchedule;
import com.bookit.schedule.entity.WorkingHoursInterval;
import com.bookit.schedule.repository.WorkScheduleRepository;
import com.bookit.schedule.service.WorkScheduleService;
import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkScheduleServiceImpl implements WorkScheduleService {

    private static final String EMPLOYEE_CONSTRAINT = "uk_work_schedules_employee";

    private final WorkScheduleRepository workScheduleRepository;
    private final EmployeeService employeeService;

    @Override
    public WorkScheduleResponse get(UUID ownerId, UUID employeeId) {
        employeeService.requireMine(ownerId, employeeId);
        return workScheduleRepository.findByEmployeeId(employeeId)
                .map(WorkScheduleResponse::from)
                .orElseGet(WorkScheduleResponse::empty);
    }

    @Override
    @Transactional
    public WorkScheduleResponse replace(UUID ownerId, UUID employeeId, WorkScheduleRequest request) {
        employeeService.requireMine(ownerId, employeeId);
        List<WorkingHoursInterval> intervals = request.toIntervals();
        WorkSchedule schedule = workScheduleRepository.findByEmployeeId(employeeId)
                .orElseGet(() -> new WorkSchedule(employeeId));
        schedule.replaceWeek(intervals);
        try {
            workScheduleRepository.saveAndFlush(schedule);
        } catch (DataIntegrityViolationException ex) {
            throw translateConflict(ex);
        }
        return WorkScheduleResponse.from(schedule);
    }

    @Override
    public List<WorkingHoursInterval> getIntervals(UUID employeeId, DayOfWeek day) {
        return workScheduleRepository.findByEmployeeId(employeeId)
                .map(schedule -> schedule.getIntervals().stream()
                        .filter(interval -> interval.dayOfWeek() == day)
                        .toList())
                .orElseGet(List::of);
    }

    private static RuntimeException translateConflict(DataIntegrityViolationException ex) {
        if (ex.getCause() instanceof ConstraintViolationException cause
                && EMPLOYEE_CONSTRAINT.equals(cause.getConstraintName())) {
            return new OptimisticLockingFailureException("Concurrent first save of the work schedule", ex);
        }
        return ex;
    }
}
