package com.bookit.appointment.service.impl;

import com.bookit.appointment.dto.AppointmentResponse;
import com.bookit.appointment.entity.Appointment;
import com.bookit.appointment.repository.AppointmentRepository;
import com.bookit.employee.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class AppointmentWriter {

    private final AppointmentRepository appointmentRepository;
    private final EmployeeService employeeService;

    @Transactional
    public AppointmentResponse insert(Appointment appointment) {
        if (!employeeService.lockIfActive(appointment.getEmployeeId())) {
            throw AvailabilityServiceImpl.slotTaken();
        }
        return AppointmentResponse.from(appointmentRepository.saveAndFlush(appointment));
    }
}
