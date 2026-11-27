package com.bookit.employee.service;

import java.util.UUID;

public interface UpcomingAppointments {

    long countForEmployee(UUID employeeId);
}
