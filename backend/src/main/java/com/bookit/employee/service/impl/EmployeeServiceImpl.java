package com.bookit.employee.service.impl;

import com.bookit.business.service.BusinessService;
import com.bookit.employee.dto.EmployeeRequest;
import com.bookit.employee.dto.EmployeeResponse;
import com.bookit.employee.dto.PublicEmployeeResponse;
import com.bookit.employee.entity.Employee;
import com.bookit.employee.repository.EmployeeRepository;
import com.bookit.employee.service.EmployeeService;
import com.bookit.offering.service.OfferingService;
import com.bookit.shared.error.BusinessRuleException;
import com.bookit.shared.error.InvalidFieldException;
import com.bookit.shared.error.ResourceNotFoundException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private static final String DISPLAY_NAME_CONSTRAINT = "uk_employees_business_display_name_active";

    private final EmployeeRepository employeeRepository;
    private final BusinessService businessService;
    private final OfferingService offeringService;

    @Override
    public List<EmployeeResponse> getMine(UUID ownerId) {
        UUID businessId = businessService.getMyBusinessId(ownerId);
        return employeeRepository.findByBusinessIdAndActiveTrueOrderByDisplayNameAsc(businessId).stream()
                .map(EmployeeResponse::from)
                .toList();
    }

    @Override
    public EmployeeResponse create(UUID ownerId, EmployeeRequest request) {
        UUID businessId = businessService.getMyBusinessId(ownerId);
        Employee employee = new Employee(businessId, request.displayName(), request.position());
        try {
            employeeRepository.saveAndFlush(employee);
        } catch (DataIntegrityViolationException ex) {
            throw translateDisplayNameConflict(ex);
        }
        return EmployeeResponse.from(employee);
    }

    @Override
    @Transactional
    public EmployeeResponse update(UUID ownerId, UUID employeeId, EmployeeRequest request) {
        Employee employee = findMine(businessService.getMyBusinessId(ownerId), employeeId);
        employee.update(request.displayName(), request.position());
        try {
            employeeRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw translateDisplayNameConflict(ex);
        }
        return EmployeeResponse.from(employee);
    }

    @Override
    @Transactional
    public EmployeeResponse assignOfferings(UUID ownerId, UUID employeeId, Set<UUID> offeringIds) {
        UUID businessId = businessService.getMyBusinessId(ownerId);
        Employee employee = findMine(businessId, employeeId);
        if (!offeringService.allActiveInBusiness(offeringIds, businessId)) {
            throw new InvalidFieldException("offeringIds", "Unknown offering");
        }
        employee.replaceOfferings(offeringIds);
        return EmployeeResponse.from(employee);
    }

    @Override
    @Transactional
    public void delete(UUID ownerId, UUID employeeId) {
        findMine(businessService.getMyBusinessId(ownerId), employeeId).deactivate();
    }

    @Override
    public List<PublicEmployeeResponse> getPublic(String businessSlug) {
        UUID businessId = businessService.getPublicBusinessId(businessSlug);
        return employeeRepository.findByBusinessIdAndActiveTrueOrderByDisplayNameAsc(businessId).stream()
                .map(PublicEmployeeResponse::from)
                .toList();
    }

    private Employee findMine(UUID businessId, UUID employeeId) {
        return employeeRepository.findByIdAndBusinessIdAndActiveTrue(employeeId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));
    }

    private static RuntimeException translateDisplayNameConflict(DataIntegrityViolationException ex) {
        if (ex.getCause() instanceof ConstraintViolationException cause
                && DISPLAY_NAME_CONSTRAINT.equals(cause.getConstraintName())) {
            return new BusinessRuleException("EMPLOYEE_NAME_TAKEN", "An employee with this name already exists");
        }
        return ex;
    }
}
