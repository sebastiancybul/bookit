package com.bookit.schedule.service.impl;

import static java.time.DayOfWeek.MONDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bookit.employee.service.EmployeeService;
import com.bookit.schedule.dto.WorkScheduleRequest;
import com.bookit.schedule.dto.WorkScheduleResponse;
import com.bookit.schedule.dto.WorkingHoursIntervalDto;
import com.bookit.schedule.entity.WorkSchedule;
import com.bookit.schedule.entity.WorkingHoursInterval;
import com.bookit.schedule.repository.WorkScheduleRepository;
import com.bookit.shared.error.InvalidFieldException;
import com.bookit.shared.error.ResourceNotFoundException;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class WorkScheduleServiceImplTest {

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();

    @Mock
    WorkScheduleRepository workScheduleRepository;

    @Mock
    EmployeeService employeeService;

    @InjectMocks
    WorkScheduleServiceImpl service;

    @Test
    void getReturnsEmptyWeekWhenEmployeeHasNoScheduleYet() {
        given(workScheduleRepository.findByEmployeeId(EMPLOYEE_ID)).willReturn(Optional.empty());

        WorkScheduleResponse response = service.get(OWNER_ID, EMPLOYEE_ID);

        assertThat(response.intervals()).isEmpty();
        then(employeeService).should().requireMine(OWNER_ID, EMPLOYEE_ID);
    }

    @Test
    void getOfForeignEmployeeFailsBeforeReadingSchedule() {
        willThrow(new ResourceNotFoundException("Employee", EMPLOYEE_ID))
                .given(employeeService).requireMine(OWNER_ID, EMPLOYEE_ID);

        assertThatThrownBy(() -> service.get(OWNER_ID, EMPLOYEE_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(workScheduleRepository);
    }

    @Test
    void replaceCreatesScheduleForEmployeeWhenMissing() {
        given(workScheduleRepository.findByEmployeeId(EMPLOYEE_ID)).willReturn(Optional.empty());
        ArgumentCaptor<WorkSchedule> saved = ArgumentCaptor.forClass(WorkSchedule.class);

        WorkScheduleResponse response = service.replace(OWNER_ID, EMPLOYEE_ID, request(dto(1, "09:00", "17:00")));

        then(workScheduleRepository).should().saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEmployeeId()).isEqualTo(EMPLOYEE_ID);
        assertThat(saved.getValue().getIntervals()).containsExactly(
                new WorkingHoursInterval(MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)));
        assertThat(response.intervals()).containsExactly(dto(1, "09:00", "17:00"));
    }

    @Test
    void replaceUpdatesExistingSchedule() {
        WorkSchedule existing = new WorkSchedule(EMPLOYEE_ID);
        existing.replaceWeek(List.of(new WorkingHoursInterval(MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0))));
        given(workScheduleRepository.findByEmployeeId(EMPLOYEE_ID)).willReturn(Optional.of(existing));

        service.replace(OWNER_ID, EMPLOYEE_ID, request(dto(5, "10:00", "14:00")));

        then(workScheduleRepository).should().saveAndFlush(existing);
        assertThat(existing.getIntervals()).extracting(WorkingHoursInterval::startTime)
                .containsExactly(LocalTime.of(10, 0));
    }

    @Test
    void replaceForForeignEmployeeFailsBeforeValidatingOrSaving() {
        willThrow(new ResourceNotFoundException("Employee", EMPLOYEE_ID))
                .given(employeeService).requireMine(OWNER_ID, EMPLOYEE_ID);

        assertThatThrownBy(() -> service.replace(OWNER_ID, EMPLOYEE_ID, request(dto(1, "17:00", "09:00"))))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(workScheduleRepository);
    }

    @Test
    void replaceWithInvalidIntervalDoesNotSave() {
        assertThatThrownBy(() -> service.replace(OWNER_ID, EMPLOYEE_ID, request(dto(1, "17:00", "09:00"))))
                .isInstanceOf(InvalidFieldException.class);
        then(workScheduleRepository).should(never()).saveAndFlush(any());
    }

    @Test
    void concurrentFirstSaveIsTranslatedToConcurrentUpdateConflict() {
        given(workScheduleRepository.findByEmployeeId(EMPLOYEE_ID)).willReturn(Optional.empty());
        given(workScheduleRepository.saveAndFlush(any())).willThrow(integrityViolation("uk_work_schedules_employee"));

        assertThatThrownBy(() -> service.replace(OWNER_ID, EMPLOYEE_ID, request(dto(1, "09:00", "17:00"))))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void otherIntegrityViolationIsRethrownUnchanged() {
        DataIntegrityViolationException violation = integrityViolation("ck_work_schedule_intervals_time_range");
        given(workScheduleRepository.findByEmployeeId(EMPLOYEE_ID)).willReturn(Optional.empty());
        given(workScheduleRepository.saveAndFlush(any())).willThrow(violation);

        assertThatThrownBy(() -> service.replace(OWNER_ID, EMPLOYEE_ID, request(dto(1, "09:00", "17:00"))))
                .isSameAs(violation);
    }

    private static DataIntegrityViolationException integrityViolation(String constraint) {
        return new DataIntegrityViolationException("violation",
                new ConstraintViolationException("violation", new SQLException(), constraint));
    }

    private static WorkScheduleRequest request(WorkingHoursIntervalDto... intervals) {
        return new WorkScheduleRequest(List.of(intervals));
    }

    private static WorkingHoursIntervalDto dto(int day, String start, String end) {
        return new WorkingHoursIntervalDto(day, LocalTime.parse(start), LocalTime.parse(end));
    }
}
