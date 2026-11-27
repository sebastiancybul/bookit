package com.bookit.appointment.service.impl;

import com.bookit.appointment.entity.AppointmentStatus;
import com.bookit.appointment.repository.AppointmentRepository;
import com.bookit.employee.service.UpcomingAppointments;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class UpcomingAppointmentsImpl implements UpcomingAppointments {

    private final AppointmentRepository appointmentRepository;
    private final Clock clock;

    @Override
    public long countForEmployee(UUID employeeId) {
        return appointmentRepository.countByEmployeeIdAndStatusAndEndTimeAfter(
                employeeId, AppointmentStatus.CONFIRMED, clock.instant());
    }
}
