package com.bookit.employee.service;

import com.bookit.employee.dto.EmployeeRequest;
import com.bookit.employee.dto.EmployeeResponse;
import com.bookit.employee.dto.PublicEmployeeResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface EmployeeService {

    List<EmployeeResponse> getMine(UUID ownerId);

    EmployeeResponse create(UUID ownerId, EmployeeRequest request);

    EmployeeResponse update(UUID ownerId, UUID employeeId, EmployeeRequest request);

    EmployeeResponse assignOfferings(UUID ownerId, UUID employeeId, Set<UUID> offeringIds);

    void delete(UUID ownerId, UUID employeeId);

    List<PublicEmployeeResponse> getPublic(String businessSlug);
}
